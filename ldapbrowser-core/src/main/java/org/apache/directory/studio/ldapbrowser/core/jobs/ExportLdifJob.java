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

package org.apache.directory.studio.ldapbrowser.core.jobs;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.PagedResultsResponseControl;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.studio.connection.core.Connection;
import org.apache.directory.studio.connection.core.io.jndi.StudioNamingEnumeration;
import org.apache.directory.studio.connection.core.jobs.StudioProgressMonitor;
import org.apache.directory.studio.ldapbrowser.core.BrowserCoreMessages;
import org.apache.directory.studio.ldapbrowser.core.model.ConnectionException;
import org.apache.directory.studio.ldapbrowser.core.model.IBrowserConnection;
import org.apache.directory.studio.ldapbrowser.core.model.SearchParameter;
import org.apache.directory.studio.ldapbrowser.core.model.StudioControl;
import org.apache.directory.studio.ldapbrowser.core.model.StudioPagedResultsControl;
import org.apache.directory.studio.ldapbrowser.core.utils.AttributeComparator;
import org.apache.directory.studio.ldapbrowser.core.utils.JNDIUtils;
import org.apache.directory.studio.ldapbrowser.core.utils.Utils;
import org.apache.directory.studio.ldifparser.model.LdifEnumeration;
import org.apache.directory.studio.ldifparser.model.container.LdifContainer;
import org.apache.directory.studio.ldifparser.model.container.LdifContentRecord;
import org.apache.directory.studio.ldifparser.model.lines.LdifAttrValLine;
import org.apache.directory.studio.ldifparser.model.lines.LdifDnLine;
import org.apache.directory.studio.ldifparser.model.lines.LdifSepLine;


/**
 * Job to export directory content to an LDIF file.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ExportLdifJob extends AbstractEclipseJob
{

    /** The filename of the LDIF file. */
    private String exportLdifFilename;

    /** The browser connection. */
    private IBrowserConnection browserConnection;

    /** The search parameter. */
    private SearchParameter searchParameter;


    /**
     * Creates a new instance of ExportLdifJob.
     * 
     * @param exportLdifFilename the filename of the LDIF file
     * @param browserConnection the browser connection
     * @param searchParameter the search parameter
     */
    public ExportLdifJob( String exportLdifFilename, IBrowserConnection browserConnection,
        SearchParameter searchParameter )
    {
        this.exportLdifFilename = exportLdifFilename;
        this.browserConnection = browserConnection;
        this.searchParameter = searchParameter;

        setName( BrowserCoreMessages.jobs__export_ldif_name );
    }


    /**
     * @see org.apache.directory.studio.ldapbrowser.core.jobs.AbstractEclipseJob#getConnections()
     */
    protected Connection[] getConnections()
    {
        return new Connection[]
            { browserConnection.getConnection() };
    }


    /**
     * @see org.apache.directory.studio.ldapbrowser.core.jobs.AbstractEclipseJob#getLockedObjects()
     */
    protected Object[] getLockedObjects()
    {
        List<Object> l = new ArrayList<Object>();
        l.add( browserConnection.getUrl() + "_" + DigestUtils.shaHex( exportLdifFilename ) );
        return l.toArray();
    }


    /**
     * @see org.apache.directory.studio.ldapbrowser.core.jobs.AbstractEclipseJob#executeAsyncJob(org.apache.directory.studio.connection.core.jobs.StudioProgressMonitor)
     */
    protected void executeAsyncJob( StudioProgressMonitor monitor )
    {
        monitor.beginTask( BrowserCoreMessages.jobs__export_ldif_task, 2 );
        monitor.reportProgress( " " ); //$NON-NLS-1$
        monitor.worked( 1 );

        try
        {
            // open file
            FileWriter fileWriter = new FileWriter( exportLdifFilename );
            BufferedWriter bufferedWriter = new BufferedWriter( fileWriter );

            // export
            int count = 0;
            export( browserConnection, searchParameter, bufferedWriter, count, monitor );

            // close file
            bufferedWriter.close();
            fileWriter.close();

        }
        catch ( Exception e )
        {
            monitor.reportError( e );
        }
    }


    private static void export( IBrowserConnection browserConnection, SearchParameter searchParameter,
        BufferedWriter bufferedWriter, int count, StudioProgressMonitor monitor ) throws IOException,
        ConnectionException
    {
        try
        {
            AttributeComparator comparator = new AttributeComparator( browserConnection );
            JndiLdifEnumeration enumeration = search( browserConnection, searchParameter, monitor );

            while ( !monitor.isCanceled() && enumeration.hasNext() )
            {
                LdifContainer container = enumeration.next();

                if ( container instanceof LdifContentRecord )
                {
                    LdifContentRecord record = ( LdifContentRecord ) container;
                    LdifDnLine dnLine = record.getDnLine();
                    LdifAttrValLine[] attrValLines = record.getAttrVals();
                    LdifSepLine sepLine = record.getSepLine();

                    // sort and format
                    Arrays.sort( attrValLines, comparator );
                    LdifContentRecord newRecord = new LdifContentRecord( dnLine );
                    for ( int i = 0; i < attrValLines.length; i++ )
                    {
                        newRecord.addAttrVal( attrValLines[i] );
                    }
                    newRecord.finish( sepLine );
                    String s = newRecord.toFormattedString( Utils.getLdifFormatParameters() );

                    // String s = record.toFormattedString();
                    bufferedWriter.write( s );

                    count++;
                    monitor.reportProgress( BrowserCoreMessages.bind( BrowserCoreMessages.jobs__export_progress,
                        new String[]
                            { Integer.toString( count ) } ) );
                }
            }
        }
        catch ( ConnectionException ce )
        {
            if ( ce.getLdapStatusCode() == ConnectionException.STAUS_CODE_TIMELIMIT_EXCEEDED
                || ce.getLdapStatusCode() == ConnectionException.STAUS_CODE_SIZELIMIT_EXCEEDED
                || ce.getLdapStatusCode() == ConnectionException.STAUS_CODE_ADMINLIMIT_EXCEEDED )
            {
                // ignore
            }
            else
            {
                monitor.reportError( ce );
            }
        }
    }


    protected String getErrorMessage()
    {
        return BrowserCoreMessages.jobs__export_ldif_error;
    }


    static JndiLdifEnumeration search( IBrowserConnection browserConnection, SearchParameter parameter,
        StudioProgressMonitor monitor ) throws ConnectionException
    {
        StudioNamingEnumeration result = SearchRunnable.search( browserConnection, parameter, monitor );

        if ( monitor.errorsReported() )
        {
            throw JNDIUtils.createConnectionException( null, monitor.getException() );
        }
        return new JndiLdifEnumeration( result, browserConnection, parameter, monitor );
    }

    static class JndiLdifEnumeration implements LdifEnumeration
    {

        private StudioNamingEnumeration enumeration;

        private IBrowserConnection browserConnection;

        private SearchParameter parameter;

        private StudioProgressMonitor monitor;


        public JndiLdifEnumeration( StudioNamingEnumeration enumeration, IBrowserConnection browserConnection,
            SearchParameter parameter, StudioProgressMonitor monitor )
        {
            this.enumeration = enumeration;
            this.browserConnection = browserConnection;
            this.parameter = parameter;
            this.monitor = monitor;
        }


        public boolean hasNext() throws ConnectionException
        {
            try
            {
                if ( enumeration != null )
                {
                    if ( enumeration.hasMore() )
                    {
                        return true;
                    }

                    Control[] jndiControls = enumeration.getResponseControls();
                    if ( jndiControls != null )
                    {
                        for ( Control jndiControl : jndiControls )
                        {
                            if ( jndiControl instanceof PagedResultsResponseControl )
                            {
                                PagedResultsResponseControl prrc = ( PagedResultsResponseControl ) jndiControl;
                                byte[] cookie = prrc.getCookie();
                                if ( cookie != null )
                                {
                                    // search again: pass the response cookie to the request control
                                    for ( StudioControl studioControl : parameter.getControls() )
                                    {
                                        if ( studioControl instanceof StudioPagedResultsControl )
                                        {
                                            StudioPagedResultsControl sprc = ( StudioPagedResultsControl ) studioControl;
                                            sprc.setCookie( cookie );
                                        }
                                    }
                                    enumeration = SearchRunnable.search( browserConnection, parameter, monitor );
                                    return enumeration != null && enumeration.hasMore();
                                }
                            }
                        }
                    }
                }

                return false;
            }
            catch ( NamingException e )
            {
                throw JNDIUtils.createConnectionException( parameter, e );
            }
        }


        public LdifContainer next() throws ConnectionException
        {
            try
            {
                SearchResult sr = enumeration.next();
                LdapDN dn = JNDIUtils.getDn( sr );
                LdifContentRecord record = LdifContentRecord.create( dn.toString() );

                NamingEnumeration<? extends Attribute> attributeEnumeration = sr.getAttributes().getAll();
                while ( attributeEnumeration.hasMore() )
                {
                    Attribute attribute = attributeEnumeration.next();
                    String attributeName = attribute.getID();
                    NamingEnumeration<?> valueEnumeration = attribute.getAll();
                    while ( valueEnumeration.hasMore() )
                    {
                        Object o = valueEnumeration.next();
                        if ( o instanceof String )
                        {
                            record.addAttrVal( LdifAttrValLine.create( attributeName, ( String ) o ) );
                        }
                        if ( o instanceof byte[] )
                        {
                            record.addAttrVal( LdifAttrValLine.create( attributeName, ( byte[] ) o ) );
                        }
                    }
                }

                record.finish( LdifSepLine.create() );

                return record;

            }
            catch ( NamingException e )
            {
                throw JNDIUtils.createConnectionException( parameter, e );
            }
        }
    }

}
