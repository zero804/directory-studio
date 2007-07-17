/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.studio.apacheds.schemaeditor.controller;


import org.apache.directory.studio.apacheds.schemaeditor.controller.actions.ConnectAction;
import org.apache.directory.studio.apacheds.schemaeditor.controller.actions.DeleteSchemaElementAction;
import org.apache.directory.studio.apacheds.schemaeditor.controller.actions.ExportSchemasAsOpenLdapAction;
import org.apache.directory.studio.apacheds.schemaeditor.controller.actions.ExportSchemasAsXmlAction;
import org.apache.directory.studio.apacheds.schemaeditor.controller.actions.ImportSchemasFromOpenLdapAction;
import org.apache.directory.studio.apacheds.schemaeditor.controller.actions.ImportSchemasFromXmlAction;
import org.apache.directory.studio.apacheds.schemaeditor.controller.actions.NewAttributeTypeAction;
import org.apache.directory.studio.apacheds.schemaeditor.controller.actions.NewObjectClassAction;
import org.apache.directory.studio.apacheds.schemaeditor.controller.actions.NewSchemaAction;
import org.apache.directory.studio.apacheds.schemaeditor.controller.actions.OpenElementAction;
import org.apache.directory.studio.apacheds.schemaeditor.view.editors.attributetype.AttributeTypeEditor;
import org.apache.directory.studio.apacheds.schemaeditor.view.editors.attributetype.AttributeTypeEditorInput;
import org.apache.directory.studio.apacheds.schemaeditor.view.editors.objectclass.ObjectClassEditor;
import org.apache.directory.studio.apacheds.schemaeditor.view.editors.objectclass.ObjectClassEditorInput;
import org.apache.directory.studio.apacheds.schemaeditor.view.views.SchemaView;
import org.apache.directory.studio.apacheds.schemaeditor.view.wrappers.AttributeTypeWrapper;
import org.apache.directory.studio.apacheds.schemaeditor.view.wrappers.Folder;
import org.apache.directory.studio.apacheds.schemaeditor.view.wrappers.ObjectClassWrapper;
import org.apache.directory.studio.apacheds.schemaeditor.view.wrappers.SchemaWrapper;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;


/**
 * This class implements the Controller for the SchemaView.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SchemaViewController
{
    /** The associated view */
    private SchemaView view;

    /** The Context Menu */
    private MenuManager contextMenu;

    /** The TreeViewer */
    private TreeViewer viewer;

    // The Actions
    private Action connect;
    private NewSchemaAction newSchema;
    private NewAttributeTypeAction newAttributeType;
    private NewObjectClassAction newObjectClass;
    private OpenElementAction openElement;
    private DeleteSchemaElementAction deleteSchemaElement;
    private ImportSchemasFromOpenLdapAction importSchemasFromOpenLdap;
    private ImportSchemasFromXmlAction importSchemasFromXml;
    private ExportSchemasAsOpenLdapAction exportSchemasAsOpenLdap;
    private ExportSchemasAsXmlAction exportSchemasAsXml;


    /**
     * Creates a new instance of SchemasViewController.
     *
     * @param view
     *      the associated view
     */
    public SchemaViewController( SchemaView view )
    {
        this.view = view;
        viewer = view.getViewer();

        initActions();
        initToolbar();
        initContextMenu();
        initDoubleClickListener();
    }


    /**
     * Initializes the Actions.
     */
    private void initActions()
    {
        connect = new ConnectAction( view );
        newSchema = new NewSchemaAction();
        newAttributeType = new NewAttributeTypeAction();
        newObjectClass = new NewObjectClassAction();
        openElement = new OpenElementAction();
        deleteSchemaElement = new DeleteSchemaElementAction();
        importSchemasFromOpenLdap = new ImportSchemasFromOpenLdapAction();
        importSchemasFromXml = new ImportSchemasFromXmlAction();
        exportSchemasAsOpenLdap = new ExportSchemasAsOpenLdapAction();
        exportSchemasAsXml = new ExportSchemasAsXmlAction();
    }


    /**
     * Initializes the Toolbar.
     */
    private void initToolbar()
    {
        IToolBarManager toolbar = view.getViewSite().getActionBars().getToolBarManager();
        toolbar.add( connect );
        toolbar.add( newSchema );
        toolbar.add( newAttributeType );
        toolbar.add( newObjectClass );
    }


    /**
     * Initializes the ContextMenu.
     */
    private void initContextMenu()
    {
        contextMenu = new MenuManager( "" ); //$NON-NLS-1$
        contextMenu.setRemoveAllWhenShown( true );
        contextMenu.addMenuListener( new IMenuListener()
        {
            public void menuAboutToShow( IMenuManager manager )
            {
                MenuManager newManager = new MenuManager( "New" );
                MenuManager importManager = new MenuManager( "Import..." );
                MenuManager exportManager = new MenuManager( "Export..." );
                manager.add( newManager );
                newManager.add( newSchema );
                newManager.add( newAttributeType );
                newManager.add( newObjectClass );
                manager.add( new Separator() );
                manager.add( openElement );
                manager.add( new Separator() );
                manager.add( deleteSchemaElement );
                manager.add( new Separator() );
                manager.add( importManager );
                importManager.add( importSchemasFromOpenLdap );
                importManager.add( importSchemasFromXml );
                manager.add( exportManager );
                exportManager.add( exportSchemasAsOpenLdap );
                exportManager.add( exportSchemasAsXml );

                manager.add( new Separator( IWorkbenchActionConstants.MB_ADDITIONS ) );
            }
        } );

        // set the context menu to the table viewer
        viewer.getControl().setMenu( contextMenu.createContextMenu( viewer.getControl() ) );

        // register the context menu to enable extension actions
        view.getSite().registerContextMenu( contextMenu, viewer );
    }


    /**
     * Initializes the DoubleClickListener.
     */
    private void initDoubleClickListener()
    {
        view.getViewer().addDoubleClickListener( new IDoubleClickListener()
        {
            public void doubleClick( DoubleClickEvent event )
            {
                IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

                TreeViewer viewer = view.getViewer();

                // What we get from the viewer is a StructuredSelection
                StructuredSelection selection = ( StructuredSelection ) event.getSelection();

                // Here's the real object (an AttributeTypeWrapper, ObjectClassWrapper or IntermediateNode)
                Object objectSelection = selection.getFirstElement();
                IEditorInput input = null;
                String editorId = null;

                // Selecting the right editor and input
                if ( objectSelection instanceof AttributeTypeWrapper )
                {
                    input = new AttributeTypeEditorInput( ( ( AttributeTypeWrapper ) objectSelection )
                        .getAttributeType() );
                    editorId = AttributeTypeEditor.ID;
                }
                else if ( objectSelection instanceof ObjectClassWrapper )
                {
                    input = new ObjectClassEditorInput( ( ( ObjectClassWrapper ) objectSelection ).getObjectClass() );
                    editorId = ObjectClassEditor.ID;
                }
                else if ( ( objectSelection instanceof Folder ) || ( objectSelection instanceof SchemaWrapper ) )
                {
                    // Here we don't open an editor, we just expand the node.
                    viewer.setExpandedState( objectSelection, !viewer.getExpandedState( objectSelection ) );
                }

                // Let's open the editor
                if ( input != null )
                {
                    try
                    {
                        page.openEditor( input, editorId );
                    }
                    catch ( PartInitException e )
                    {
                        //                        logger.debug( "error when opening the editor" ); //$NON-NLS-1$
                        e.printStackTrace();
                    }
                }
            }
        } );
    }
}
