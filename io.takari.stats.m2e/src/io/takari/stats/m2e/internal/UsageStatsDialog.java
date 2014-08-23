package io.takari.stats.m2e.internal;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FillLayout;

public class UsageStatsDialog extends TitleAreaDialog {

  public UsageStatsDialog(Shell parentShell) {
    super(parentShell);
    setHelpAvailable(false);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    setTitle("Usage Statistics Collection");
    Composite area = (Composite) super.createDialogArea(parent);
    Composite container = new Composite(area, SWT.NONE);
    container.setLayout(new FillLayout(SWT.HORIZONTAL));
    container.setLayoutData(new GridData(GridData.FILL_BOTH));

    StyledText styledText = new StyledText(container, SWT.BORDER | SWT.READ_ONLY | SWT.WRAP);
    styledText
        .setText("Takari, Inc. collects certain usage statistics. These usage statistics lets us  measure things like active usage of Takari tools, and lets us know things like which versions of the tools are in use and which tools are the most popular with developers. This limited data is examined on an aggreate basis and is maintained in accordance with Takari, Inc. Privacy Policy.\n");

    return area;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
  }

  @Override
  protected Point getInitialSize() {
    return new Point(450, 300);
  }
}
