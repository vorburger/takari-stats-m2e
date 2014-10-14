/**
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.stats.m2e.internal;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsageStatsDialog extends TitleAreaDialog {

  private final Logger log = LoggerFactory.getLogger(getClass());

  public UsageStatsDialog(Shell parentShell) {
    super(parentShell);
    setHelpAvailable(false);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    setTitle("Usage Statistics Collection");
    setMessage(UsageStatsActivator.getId());
    Composite area = (Composite) super.createDialogArea(parent);
    Composite container = new Composite(area, SWT.NONE);
    container.setLayout(new FillLayout(SWT.HORIZONTAL));
    container.setLayoutData(new GridData(GridData.FILL_BOTH));

    final IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();

    Link link = new Link(container, SWT.WRAP | SWT.NO_FOCUS);
    link.setText("Takari, Inc. collects certain usage statistics. These usage statistics lets us  measure things like active usage of Takari tools, and lets us know things like which versions of the tools are in use and which tools are the most popular with developers. This limited data is examined on an aggregate basis and is maintained in accordance with Takari, Inc. Privacy Policy. \n\n<a href=\"https://github.com/takari/takari-stats-m2e\">Github project page</a> provides information about data we collect.");
    link.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        try {
          browserSupport.getExternalBrowser().openURL(new URL(event.text));
        } catch (PartInitException | MalformedURLException e) {
          log.error("Could not open external browser", e);
        }
      }
    });

    return area;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true).setFocus();
  }

  @Override
  protected Point getInitialSize() {
    return new Point(450, 300);
  }
}
