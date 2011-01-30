package com.dynamo.cr.editor.wizards;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPage;

import com.dynamo.cr.editor.Activator;


public class ConnectionWizard extends Wizard {

    private ConnectionWizardPageView view;
    private ConnectionWizardPagePresenter presenter;
    private IWorkbenchPage page;

    public ConnectionWizard(IWorkbenchPage page) {
        this.page = page;
        setWindowTitle("Connect to resource server");
    }

    @Override
    public void addPages() {
        view = new ConnectionWizardPageView("Connect");
        presenter = new ConnectionWizardPagePresenter(view, page);
        presenter.setProjectResourceClient(Activator.getDefault().projectClient);
        view.setPresenter(presenter);
        addPage(view);
    }

    @Override
    public void createPageControls(Composite pageContainer) {
        super.createPageControls(pageContainer);
        presenter.init();
    }

    @Override
    public boolean performFinish() {
        return presenter.finish();
    }
}
