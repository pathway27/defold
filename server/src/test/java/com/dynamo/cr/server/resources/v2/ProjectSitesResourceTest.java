package com.dynamo.cr.server.resources.v2;

import com.dynamo.cr.protocol.proto.Protocol;
import com.dynamo.cr.server.resources.AbstractResourceTest;
import com.dynamo.cr.server.test.TestUser;
import com.dynamo.cr.server.test.TestUtils;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.multipart.MultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ProjectSitesResourceTest extends AbstractResourceTest {

    @BeforeClass
    public static void beforeClass() throws IOException {
        execCommand("scripts/setup_template_project.sh", "proj1");

        EntityManager entityManager = emf.createEntityManager();
        entityManager.getTransaction().begin();
        TestUtils.createTestUsers(entityManager);
        entityManager.getTransaction().commit();
    }

    private Protocol.ProjectInfo createProject(TestUser owner) {
        WebResource baseResource = createBaseResource(owner.email, owner.password);

        Protocol.NewProject newProject = Protocol.NewProject.newBuilder()
                .setName("name")
                .setDescription("description")
                .setTemplateId("proj1")
                .build();

        return baseResource.path("/projects/-1/").post(Protocol.ProjectInfo.class, newProject);
    }

    private void addProjectMember(Long projectId, TestUser owner, TestUser newMember) {
        WebResource baseResource = createBaseResource(owner.email, owner.password)
                .path("/projects/-1")
                .path(String.valueOf(projectId))
                .path("members");

        baseResource.post(newMember.email);
    }

    private WebResource projectSiteResource(TestUser user, Long projectId) {
        return createBaseResource(user.email, user.password).path("v2/projects").path(projectId.toString()).path("site");
    }

    private WebResource projectSiteResourceWithFaultyPassword(Long projectId) {
        return createBaseResource("FAULTY", "FAULTY").path("v2/projects").path(projectId.toString()).path("site");
    }

    private WebResource projectSitesResource(TestUser user) {
        return createBaseResource(user.email, user.password).path("v2/projects").path("sites");
    }

    private WebResource projectSiteResource(Long projectId) {
        return createAnonymousResource().path("v2/projects").path(projectId.toString()).path("site");
    }

    private void updateProjectSite(TestUser testUser, Long projectId, Protocol.ProjectSite projectSite) {
        projectSiteResource(testUser, projectId).put(projectSite);
    }

    private Protocol.ProjectSite getProjectSite(TestUser testUser, Long projectId) {
        return projectSiteResource(testUser, projectId).get(Protocol.ProjectSite.class);
    }

    private Protocol.ProjectSiteList getProjectSites(TestUser testUser) {
        return projectSitesResource(testUser).get(Protocol.ProjectSiteList.class);
    }

    private Protocol.ProjectSite getProjectSite(Long projectId) {
        return projectSiteResource(projectId).get(Protocol.ProjectSite.class);
    }

    private Protocol.ProjectSite getProjectSiteWithFaultyPassword(Long projectId) {
        return projectSiteResourceWithFaultyPassword(projectId).get(Protocol.ProjectSite.class);
    }

    private void addAppStoreReference(TestUser testUser, Long projectId, Protocol.NewAppStoreReference newAppStoreReference) {
        projectSiteResource(testUser, projectId).path("app_store_references").post(newAppStoreReference);
    }

    private void deleteAppStoreReference(TestUser testUser, Long projectId, Long appStoreReferenceId) {
        projectSiteResource(testUser, projectId).path("app_store_references").path(appStoreReferenceId.toString()).delete();
    }

    private void addScreenshot(TestUser testUser, Long projectId, Protocol.NewScreenshot newScreenshot) {
        projectSiteResource(testUser, projectId).path("screenshots").post(newScreenshot);
    }

    private void deleteScreenshot(TestUser testUser, Long projectId, Long screenshotId) {
        projectSiteResource(testUser, projectId).path("screenshots").path(screenshotId.toString()).delete();
    }

    private void orderScreenshots(TestUser testUser, Long projectId, List<Long> screenshotIds) {
        Protocol.ScreenshotSortOrderRequest screenshotSortOrderRequest = Protocol.ScreenshotSortOrderRequest.newBuilder()
                .addAllScreenshotIds(screenshotIds)
                .build();

        projectSiteResource(testUser, projectId).path("screenshots").path("order").put(screenshotSortOrderRequest);
    }

    private void addSocialMediaReference(TestUser testUser, Long projectId, Protocol.NewSocialMediaReference newSocialMediaReference) {
        projectSiteResource(testUser, projectId).path("social_media_references").post(newSocialMediaReference);
    }

    private void deleteSocialMediaReference(TestUser testUser, Long projectId, Long socialMediaReferenceId) {
        projectSiteResource(testUser, projectId).path("social_media_references").path(socialMediaReferenceId.toString()).delete();
    }

    private void likeProjectSite(TestUser testUser, Long projectId) {
        projectSiteResource(testUser, projectId).path("like").put();
    }

    private void unlikeProjectSite(TestUser testUser, Long projectId) {
        projectSiteResource(testUser, projectId).path("unlike").put();
    }

    @Test
    public void listProjectSites() {
        int originalSitesCount = getProjectSites(TestUser.JAMES).getSitesCount();

        Protocol.ProjectInfo project = createProject(TestUser.JAMES);

        Protocol.ProjectSite projectSite = Protocol.ProjectSite.newBuilder()
                .setName("name")
                .setDescription("description")
                .setStudioUrl("studioUrl")
                .setStudioName("studioName")
                .setDevLogUrl("devLogUrl")
                .setReviewUrl("reviewUrl")
                .setLibraryUrl("libraryUrl")
                .setIsPublicSite(true)
                .build();

        updateProjectSite(TestUser.JAMES, project.getId(), projectSite);

        createProject(TestUser.JAMES);

        Protocol.ProjectSiteList result = getProjectSites(TestUser.JAMES);

        assertEquals(originalSitesCount + 1, result.getSitesCount());
    }

    @Test
    public void accessPublicSiteWithExpiredSession() {
        Protocol.ProjectInfo project = createProject(TestUser.JAMES);

        Protocol.ProjectSite projectSiteUpdate = Protocol.ProjectSite.newBuilder()
                .setIsPublicSite(true)
                .build();

        updateProjectSite(TestUser.JAMES, project.getId(), projectSiteUpdate);


        Protocol.ProjectSite projectSite = getProjectSiteWithFaultyPassword(project.getId());
        assertNotNull(projectSite);
    }

    @Test
    public void updateProjectSite() {

        Protocol.ProjectInfo project = createProject(TestUser.JAMES);

        Protocol.ProjectSite projectSite = Protocol.ProjectSite.newBuilder()
                .setName("name")
                .setDescription("description")
                .setStudioUrl("studioUrl")
                .setStudioName("studioName")
                .setDevLogUrl("devLogUrl")
                .setReviewUrl("reviewUrl")
                .setLibraryUrl("libraryUrl")
                .setShowName(false)
                .setAllowComments(false)
                .setProjectUrl("projectUrl")
                .build();

        updateProjectSite(TestUser.JAMES, project.getId(), projectSite);

        Protocol.ProjectSite result = getProjectSite(TestUser.JAMES, project.getId());

        assertEquals("name", result.getName());
        assertEquals("description", result.getDescription());
        assertEquals("studioUrl", result.getStudioUrl());
        assertEquals("studioName", result.getStudioName());
        assertEquals("devLogUrl", result.getDevLogUrl());
        assertEquals("reviewUrl", result.getReviewUrl());
        assertEquals("libraryUrl", result.getLibraryUrl());
        assertEquals(false, result.getIsPublicSite()); // Project site should be private by default.
        assertEquals(false, result.getShowName());
        assertEquals(false, result.getAllowComments());
        assertEquals("projectUrl", result.getProjectUrl());
        assertEquals(project.getId(), result.getProjectId());
    }

    @Test
    public void privateSiteShouldNotBeAccessibleForNonMembers() {
        Protocol.ProjectInfo project = createProject(TestUser.JAMES);

        Protocol.ProjectSite projectSite = Protocol.ProjectSite.newBuilder()
                .setName("name")
                .setDescription("description")
                .setStudioUrl("studioUrl")
                .setStudioName("studioName")
                .setDevLogUrl("devLogUrl")
                .setReviewUrl("reviewUrl")
                .setLibraryUrl("libraryUrl")
                .setIsPublicSite(false)
                .build();

        updateProjectSite(TestUser.JAMES, project.getId(), projectSite);

        assertNotNull(getProjectSite(TestUser.JAMES, project.getId()));

        try {
            getProjectSite(TestUser.CARL, project.getId());
        } catch (Exception e) {
            return;
        }
        fail();
    }

    @Test
    public void projectMembersAreSiteAdmins() {
        Protocol.ProjectInfo project = createProject(TestUser.JAMES);
        addProjectMember(project.getId(), TestUser.JAMES, TestUser.CARL);

        Protocol.ProjectSite projectSite = Protocol.ProjectSite.newBuilder()
                .setName("name")
                .setDescription("description")
                .setStudioUrl("studioUrl")
                .setStudioName("studioName")
                .setDevLogUrl("devLogUrl")
                .setReviewUrl("reviewUrl")
                .setLibraryUrl("libraryUrl")
                .setIsPublicSite(true)
                .build();

        updateProjectSite(TestUser.JAMES, project.getId(), projectSite);

        Protocol.ProjectSite result = getProjectSite(TestUser.JAMES, project.getId());
        assertTrue(result.getIsAdmin());

        result = getProjectSite(TestUser.CARL, project.getId());
        assertTrue(result.getIsAdmin());

        result = getProjectSite(TestUser.JOE, project.getId());
        assertFalse(result.getIsAdmin());
    }

    @Test
    public void updateAppStoreReferences() {

        Protocol.ProjectInfo project = createProject(TestUser.JAMES);

        // Add two app store references.
        Protocol.NewAppStoreReference appStoreReference = Protocol.NewAppStoreReference.newBuilder()
                .setLabel("App Store")
                .setUrl("http://www.appstore.com")
                .build();
        addAppStoreReference(TestUser.JAMES, project.getId(), appStoreReference);

        Protocol.NewAppStoreReference appStoreReference2 = Protocol.NewAppStoreReference.newBuilder()
                .setLabel("Google Play")
                .setUrl("http://www.play.com")
                .build();
        addAppStoreReference(TestUser.JAMES, project.getId(), appStoreReference2);

        // Ensure that you get two references back.
        Protocol.ProjectSite projectSite = getProjectSite(TestUser.JAMES, project.getId());
        assertEquals(2, projectSite.getAppStoreReferencesCount());

        // Delete one reference.
        long id = projectSite.getAppStoreReferences(0).getId();
        deleteAppStoreReference(TestUser.JAMES, project.getId(), id);

        // Ensure that you get one reference back.
        projectSite = getProjectSite(TestUser.JAMES, project.getId());
        assertEquals(1, projectSite.getAppStoreReferencesCount());
    }

    @Test
    public void updateScreenshot() {

        Protocol.ProjectInfo project = createProject(TestUser.JAMES);

        // Add two screenshots.
        Protocol.NewScreenshot screenshot = Protocol.NewScreenshot.newBuilder()
                .setUrl("http://www.images.com")
                .setMediaType(Protocol.ScreenshotMediaType.IMAGE)
                .build();
        addScreenshot(TestUser.JAMES, project.getId(), screenshot);

        Protocol.NewScreenshot screenshot2 = Protocol.NewScreenshot.newBuilder()
                .setUrl("http://www.images.com/2")
                .setMediaType(Protocol.ScreenshotMediaType.YOUTUBE)
                .build();
        addScreenshot(TestUser.JAMES, project.getId(), screenshot2);

        // Ensure that you get two screenshots back.
        Protocol.ProjectSite projectSite = getProjectSite(TestUser.JAMES, project.getId());
        assertEquals(2, projectSite.getScreenshotsCount());

        // Delete one screenshot.
        long id = projectSite.getScreenshots(0).getId();
        deleteScreenshot(TestUser.JAMES, project.getId(), id);

        // Ensure that you get one reference back.
        projectSite = getProjectSite(TestUser.JAMES, project.getId());
        assertEquals(1, projectSite.getScreenshotsCount());
    }

    @Test
    public void sortScreenshots() {

        Protocol.ProjectInfo project = createProject(TestUser.JAMES);

        // Add three screenshots.
        Protocol.NewScreenshot screenshot = Protocol.NewScreenshot.newBuilder()
                .setUrl("http://www.images.com")
                .setMediaType(Protocol.ScreenshotMediaType.IMAGE)
                .build();
        addScreenshot(TestUser.JAMES, project.getId(), screenshot);

        Protocol.NewScreenshot screenshot2 = Protocol.NewScreenshot.newBuilder()
                .setUrl("http://www.images.com/2")
                .setMediaType(Protocol.ScreenshotMediaType.YOUTUBE)
                .build();
        addScreenshot(TestUser.JAMES, project.getId(), screenshot2);

        Protocol.NewScreenshot screenshot3 = Protocol.NewScreenshot.newBuilder()
                .setUrl("http://www.images.com/3")
                .setMediaType(Protocol.ScreenshotMediaType.YOUTUBE)
                .build();
        addScreenshot(TestUser.JAMES, project.getId(), screenshot3);

        // Sort
        Protocol.ProjectSite projectSite = getProjectSite(TestUser.JAMES, project.getId());
        List<Long> screenshotIds = new ArrayList<>();
        screenshotIds.add(projectSite.getScreenshots(1).getId());
        screenshotIds.add(projectSite.getScreenshots(0).getId());
        screenshotIds.add(projectSite.getScreenshots(2).getId());
        orderScreenshots(TestUser.JAMES, project.getId(), screenshotIds);

        // Ensure that you get two screenshots back.
        projectSite = getProjectSite(TestUser.JAMES, project.getId());
        assertEquals(3, projectSite.getScreenshotsCount());
        assertEquals(screenshotIds.get(0).longValue(), projectSite.getScreenshots(0).getId());
        assertEquals(screenshotIds.get(1).longValue(), projectSite.getScreenshots(1).getId());
        assertEquals(screenshotIds.get(2).longValue(), projectSite.getScreenshots(2).getId());
    }

    @Test
    public void addAndDeleteSocialMediaReferences() {

        Protocol.ProjectInfo project = createProject(TestUser.JAMES);

        // Add social media reference
        String label = "Facebook";
        String url = "https://www.facebook.com/my-game";
        Protocol.NewSocialMediaReference socialMediaReference = Protocol.NewSocialMediaReference.newBuilder()
                .setLabel(label)
                .setUrl(url)
                .build();
        addSocialMediaReference(TestUser.JAMES, project.getId(), socialMediaReference);

        // Ensure that you get once reference back.
        Protocol.ProjectSite projectSite = getProjectSite(TestUser.JAMES, project.getId());
        assertEquals(1, projectSite.getSocialMediaReferencesCount());
        assertEquals(label, projectSite.getSocialMediaReferences(0).getLabel());
        assertEquals(url, projectSite.getSocialMediaReferences(0).getUrl());

        // Delete reference.
        long id = projectSite.getSocialMediaReferences(0).getId();
        deleteSocialMediaReference(TestUser.JAMES, project.getId(), id);

        // Ensure that you get zero references back.
        projectSite = getProjectSite(TestUser.JAMES, project.getId());
        assertEquals(0, projectSite.getSocialMediaReferencesCount());
    }

    @Test
    public void getProjectSiteAsUnauthorizedUser() {
        Protocol.ProjectInfo project = createProject(TestUser.JAMES);

        Protocol.ProjectSite projectSite = Protocol.ProjectSite.newBuilder()
                .setName("name")
                .setDescription("description")
                .setStudioUrl("studioUrl")
                .setStudioName("studioName")
                .setDevLogUrl("devLogUrl")
                .setReviewUrl("reviewUrl")
                .setLibraryUrl("libraryUrl")
                .setIsPublicSite(true)
                .build();

        updateProjectSite(TestUser.JAMES, project.getId(), projectSite);

        assertNotNull(getProjectSite(project.getId()));
    }

    @Test
    public void likeAndUnlikeProject() {
        // Create project as James
        Protocol.ProjectInfo project = createProject(TestUser.JAMES);

        // Like the project as James
        likeProjectSite(TestUser.JAMES, project.getId());

        // Assert that project is liked by James.
        Protocol.ProjectSite projectSite = getProjectSite(TestUser.JAMES, project.getId());
        assertTrue(projectSite.getLikedByMe());
        assertEquals(1, projectSite.getNumberOfLikes());

        // Unlike project
        unlikeProjectSite(TestUser.JAMES, project.getId());

        // Assert that project is not liked by James.
        projectSite = getProjectSite(TestUser.JAMES, project.getId());
        assertFalse(projectSite.getLikedByMe());
    }

    @Test
    public void likingProjectWithoutAccessDoesNotCount() {
        // Create project as James
        Protocol.ProjectInfo project = createProject(TestUser.JAMES);

        // Like the project as Carl
        likeProjectSite(TestUser.CARL, project.getId());

        // Assert that project still has zero likes.
        Protocol.ProjectSite projectSite = getProjectSite(TestUser.JAMES, project.getId());
        assertEquals(0, projectSite.getNumberOfLikes());
    }

    @Test
    @Ignore("Integration test to run explicitly.")
    public void updateScreenshotImages() throws URISyntaxException {
        Protocol.ProjectInfo project = createProject(TestUser.JAMES);

        File playableFile = new File(ClassLoader.getSystemResource("defold-logo.png").toURI());

        MultiPart multiPart = new MultiPart(MediaType.MULTIPART_FORM_DATA_TYPE);
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", playableFile, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        multiPart.bodyPart(fileDataBodyPart);

        projectSiteResource(TestUser.JAMES, project.getId())
                .path("screenshots")
                .path("images")
                .type(MediaType.MULTIPART_FORM_DATA_TYPE)
                .post(multiPart);

        Protocol.ProjectSite projectSite = getProjectSite(TestUser.JAMES, project.getId());

        for (Protocol.ProjectSite.Screenshot screenshot : projectSite.getScreenshotsList()) {
            System.out.println(screenshot.getId() + " " + screenshot.getUrl());

            projectSiteResource(TestUser.JAMES, project.getId())
                    .path("screenshots")
                    .path(String.valueOf(screenshot.getId()))
                    .delete();
        }
    }

    @Test
    @Ignore("Integration test to run explicitly.")
    public void uploadPlayable() throws URISyntaxException {
        Protocol.ProjectInfo project = createProject(TestUser.JAMES);

        Protocol.ProjectSite projectSite = getProjectSite(TestUser.JAMES, project.getId());

        // Project site should not have playable yet.
        assertFalse(projectSite.hasPlayableUrl());

        File playableFile = new File(ClassLoader.getSystemResource("test_playable.zip").toURI());

        MultiPart multiPart = new MultiPart(MediaType.MULTIPART_FORM_DATA_TYPE);
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", playableFile, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        multiPart.bodyPart(fileDataBodyPart);

        projectSiteResource(TestUser.JAMES, project.getId())
                .path("playable")
                .type(MediaType.MULTIPART_FORM_DATA_TYPE)
                .put(multiPart);

        projectSite = getProjectSite(TestUser.JAMES, project.getId());
        System.out.println(projectSite.getPlayableUrl());
    }

    @Test
    @Ignore("Integration test to run explicitly.")
    public void uploadAttachment() throws URISyntaxException {
        Protocol.ProjectInfo project = createProject(TestUser.JAMES);

        File playableFile = new File(ClassLoader.getSystemResource("test_playable.zip").toURI());

        MultiPart multiPart = new MultiPart(MediaType.MULTIPART_FORM_DATA_TYPE);
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", playableFile, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        multiPart.bodyPart(fileDataBodyPart);

        projectSiteResource(TestUser.JAMES, project.getId())
                .path("attachment")
                .type(MediaType.MULTIPART_FORM_DATA_TYPE)
                .post(multiPart);

        Protocol.ProjectSite projectSite = getProjectSite(TestUser.JAMES, project.getId());
        System.out.println(projectSite.getAttachmentUrl());
    }
}
