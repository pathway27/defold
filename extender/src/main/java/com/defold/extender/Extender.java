package com.defold.extender;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.yaml.snakeyaml.Yaml;

import com.google.common.collect.ImmutableMap;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

public class Extender {

    private File src;
    private File build;
    private Platform platform;

    public Extender(Configuration config, String platform, File src) throws IOException {
        this.platform = config.getPlatforms().get(platform);
        this.src = src;
        this.build = Files.createTempDirectory("engine").toFile();
    }

    private String[] subst(String template, Map<String, Object> context) {
        String s = Mustache.compiler().compile(template).execute(context);
        return s.split(" ");
    }

    private static String exec(String...args) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        int ret = 127;
        byte[] buf = new byte[16 * 1024];
        InputStream is = p.getInputStream();

        StringBuilder sb = new StringBuilder();

        int n = 0;
        do {
            n = is.read(buf);
            if (n > 0) {
                sb.append(new String(buf, 0, n));
            }
        }
        while (n > 0);
        ret = p.waitFor();
        if (ret > 0) {
            throw new IOException(sb.toString());
        }

        return sb.toString();
    }

    private File buildEngine(List<File> libs, List<String> symbols) throws IOException, InterruptedException {
        ClassLoader cl = getClass().getClassLoader();
        File maincpp = new File(build, "main.cpp");
        File exportedcpp = new File(build, "exported_symbols.cpp");
        File exe = new File(build, "dmengine");

        FileUtils.copyURLToFile(cl.getResource("main.cpp"), maincpp);

        List<String> allSymbols = new ArrayList<>();
        allSymbols.addAll(symbols);
        allSymbols.addAll(Arrays.asList("DefaultSoundDevice", "NullSoundDevice", "AudioDecoderWav", "CrashExt", "AudioDecoderStbVorbis",  "AudioDecoderTremolo"));

        Template exportedTemplate = Mustache.compiler().compile(IOUtils.toString(this.getClass().getResourceAsStream("/exported_symbols.cpp")));
        String exported = exportedTemplate.execute(ImmutableMap.of("symbols", allSymbols));
        FileUtils.writeStringToFile(exportedcpp, exported);

        String libPathHack = "-L/Users/chmu/Downloads/new3/sdk/redistributable_bin/osx32";
        String libHack = "-lsteam_api";

        List<String> args = Arrays.asList("clang++", libPathHack, libHack, maincpp.getAbsolutePath(), exportedcpp.getAbsolutePath(), "-o", exe.getAbsolutePath(), "-framework", "Cocoa", "-framework", "OpenGL", "-framework", "OpenAL", "-framework", "AGL", "-framework", "IOKit", "-framework", "Carbon", "-framework", "CoreVideo", "-framework", "Foundation", "-framework", "AppKit", "-m32", "-stdlib=libstdc++", "-mmacosx-version-min=10.7", "-framework", "Carbon", "-L/Users/chmu/tmp/dynamo-home/lib/darwin", "-L/Users/chmu/tmp/dynamo-home/ext/lib/darwin", "-lengine", "-ladtruthext", "-lfacebookext", "-liapext", "-lpushext", "-liacext", "-lrecord", "-lgameobject", "-lddf", "-lresource", "-lgamesys", "-lgraphics", "-lphysics", "-lBulletDynamics", "-lBulletCollision", "-lLinearMath", "-lBox2D", "-lrender", "-lscript", "-lluajit-5.1", "-lextension", "-lhid", "-linput", "-lparticle", "-ldlib", "-ldmglfw", "-lgui", "-ltracking", "-lcrashext", "-lsound2", "-ltremolo", "-lvpx");
        args = new ArrayList<>(args);

        for (File f : libs) {
            args.add(f.getAbsolutePath());
        }

        exec(args.toArray(new String[args.size()]));

        return exe;
    }

    private File compileFile(int i, File f, File build) throws IOException, InterruptedException {
        List<String> includes = new ArrayList<>(Arrays.asList("/Users/chmu/tmp/dynamo-home/include", "/Users/chmu/tmp/dynamo-home/ext/include"));
        includes.add(build.getParent() + "/include"); // TODO: HACK? :-)
        File o = new File(build, String.format("%s_%d.o", f.getName(), i));
        String[] args = subst(platform.getCompile(), ImmutableMap.of("src", f, "tgt", o, "includes", includes));
        System.out.println(Arrays.toString(args));
        exec(args);
        return o;
    }

    private File buildExtension(File manifest) throws IOException, InterruptedException {
        File root = manifest.getParentFile();
        File src = new File(root, "src");
        Collection<File> srcFiles = new ArrayList<>();
        if (src.isDirectory()) {
            srcFiles = FileUtils.listFiles(src, null, true);
        }
        List<String> objs = new ArrayList<>();

        int i = 0;
        for (File f : srcFiles) {
            File o = compileFile(i, f, build);
            objs.add(o.getAbsolutePath());
            i++;
        }

        File lib = File.createTempFile("lib", ".a", build);
        lib.delete();

        String[] args = subst(platform.getLib(), ImmutableMap.of("tgt", lib, "objs", objs));
        exec(args);
        return lib;
    }

    public File buildEngine() throws IOException, InterruptedException {

        Collection<File> allFiles = FileUtils.listFiles(src, null, true);
        List<File> manifests = allFiles.stream().filter(f -> f.getName().equals("ext.manifest")).collect(Collectors.toList());

        List<String> symbols = new ArrayList<>();

        for (File f : manifests) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) new Yaml().load(FileUtils.readFileToString(f));
            symbols.add((String) m.get("name"));
        }

        List<File> libs = new ArrayList<>();
        for (File manifest : manifests) {
            File lib = buildExtension(manifest);
            libs.add(lib);
        }

        return buildEngine(libs, symbols);
    }

    public void dispose() throws IOException {
        FileUtils.deleteDirectory(build);
    }

}
