/*
 * Copyright, Aspect Security, Inc.
 *
 * This file is part of JavaSnoop.
 *
 * JavaSnoop is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JavaSnoop is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JavaSnoop.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.aspect.snoop.agent;

import com.aspect.snoop.FunctionHookInterceptor;
import com.aspect.snoop.SnoopSession;
import com.aspect.snoop.agent.manager.ClassChanges;
import com.aspect.snoop.agent.manager.InstrumentationException;
import com.aspect.snoop.agent.manager.InstrumentationManager;
import com.aspect.snoop.messages.AgentMessage;
import com.aspect.snoop.messages.agent.ExitProgramRequest;
import com.aspect.snoop.messages.agent.ExitProgramResponse;
import com.aspect.snoop.messages.agent.QueryPidRequest;
import com.aspect.snoop.messages.agent.QueryPidResponse;
import com.aspect.snoop.messages.agent.StartSnoopingRequest;
import com.aspect.snoop.messages.agent.StartSnoopingResponse;
import com.aspect.snoop.messages.agent.StopSnoopingRequest;
import com.aspect.snoop.messages.agent.StopSnoopingResponse;
import com.aspect.snoop.messages.agent.TransportSessionRequest;
import com.aspect.snoop.messages.agent.TransportSessionRequest.SessionRetrievalType;
import com.aspect.snoop.messages.agent.TransportSessionResponse;
import com.aspect.snoop.messages.UnrecognizedMessage;
import com.aspect.snoop.messages.agent.ClassBytes;
import com.aspect.snoop.messages.agent.GetClassDefinitionsRequest;
import com.aspect.snoop.messages.agent.GetClassDefinitionsResponse;
import com.aspect.snoop.messages.agent.GetClassesRequest;
import com.aspect.snoop.messages.agent.GetClassesResponse;
import com.aspect.snoop.messages.agent.GetProcessInfoRequest;
import com.aspect.snoop.messages.agent.GetProcessInfoResponse;
import com.aspect.snoop.messages.agent.StartCanaryRequest;
import com.aspect.snoop.messages.agent.StartCanaryResponse;
import com.aspect.snoop.messages.agent.StopCanaryRequest;
import com.aspect.snoop.messages.agent.StopCanaryResponse;
import com.aspect.snoop.util.CanaryUtil;
import com.aspect.snoop.util.ReflectionUtil;
import com.aspect.snoop.util.SessionPersistenceUtil;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class AgentServerThread extends AbstractServerThread {

    //private static Logger logger = Logger.getLogger(AgentServerThread.class);

    public final static String SUCCESS = "SUCCESS";
    public final static String FAIL = "FAIL";

    public int ourPort = 0xADDA; // default
    public int homePort = 0xADDA+1; // default

    private SnoopSession snoopSession;

    private InstrumentationManager manager;
    private String agentArgs;

    public static String agentJar;

    protected AgentServerThread(Instrumentation inst, String agentArgs) {
        super();
        this.agentArgs = agentArgs;

        try {

            /*
            try {
               inst.appendToSystemClassLoaderSearch(new JarFile("C:/p42/dev/JavaSnoop/main/lib/javassist.jar"));
            } catch (IOException ex) {
                Logger.getLogger(AgentServerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            */
            
            String s[] = this.agentArgs.split(",");
            ourPort = Integer.parseInt(s[0]);
            homePort = Integer.parseInt(s[1]);
            agentJar = s[2];

            System.out.println("Our port: "+ourPort +", home port: "+homePort);

        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
        }

        this.manager = new InstrumentationManager(inst);
    }

    /**
     * Too complicated.  Why not just send a serialized object for every message?
     *
     * A.D. - Because it limits us to Java!
     *
     * @param command
     * @param reader
     * @param writer
     * @throws IOException
     */
    protected void processCommand(AgentMessage message, ObjectInputStream input, ObjectOutputStream output) throws IOException {

        if ( message instanceof TransportSessionRequest ) {

            TransportSessionRequest request = (TransportSessionRequest)message;
            TransportSessionResponse response = new TransportSessionResponse();

            try {

                if ( SessionRetrievalType.LoadFromXMLString.equals(request.getType()) ) {

                    String xml = request.getXML(); // the XML comes on one line only
                    StringReader sr = new StringReader(xml);
                    snoopSession = SessionPersistenceUtil.loadSession(sr);

                } else if ( SessionRetrievalType.LoadFromFile.equals(request.getType()) ) {

                    String xmlFile = request.getFilename();
                    snoopSession = SessionPersistenceUtil.loadSession(xmlFile);

                } else if ( SessionRetrievalType.LoadFromObject.equals(request.getType()) ) {

                    snoopSession = request.getSnoopSession();

                }
                
            } catch (Exception e) {
                populateResponse(response, e);
            }

            output.writeObject(response);
            
        } else if ( message instanceof StartSnoopingRequest ) {

            StartSnoopingResponse response = new StartSnoopingResponse();

            try {

                AgentToSnoopClient.initialize("localhost", homePort); // have to make dynamic eventually

                uninstallHooks();
                installHooks();

            } catch(Exception e) {
                populateResponse(response, e);
            }

            output.writeObject(response);

        } else if ( message instanceof StopSnoopingRequest ) {
            
            StopSnoopingResponse response = new StopSnoopingResponse();

            try {

                AgentToSnoopClient.finished();

                // if currently instrumenting, stops
                uninstallHooks();
                
            } catch(Exception e) {
                populateResponse(response, e);
            }

            output.writeObject(response);

        } else if ( message instanceof QueryPidRequest ) {

            QueryPidResponse response = new QueryPidResponse();

            String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
            response.setPid(pid);
            
            output.writeObject(response);

        } else if ( message instanceof GetProcessInfoRequest ) {

            GetProcessInfoResponse response = new GetProcessInfoResponse();

            ProcessInfo info = new ProcessInfo();

            String path = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();

            info.setCmd( path );
            info.setClasspath(System.getProperty("java.class.path"));
            
            response.setProcessInfo(info);

            output.writeObject(response);

        } else if ( message instanceof GetClassesRequest ){
            
            GetClassesResponse response = new GetClassesResponse();

            List<String> classes = manager.getLoadedClassesAsStrings();

            Collections.sort(classes);

            response.setClasses(classes);
            
            output.writeObject(response);

        } else if ( message instanceof GetClassDefinitionsRequest ) {

            GetClassDefinitionsRequest request = (GetClassDefinitionsRequest)message;
            GetClassDefinitionsResponse response = new GetClassDefinitionsResponse();

            String[] classes = request.getClasses();

            List<ClassBytes> list = new ArrayList<ClassBytes>();

            int i=0;
            for( String clazz : classes ) {
                i++;
                byte[] bytes = manager.getClassBytes(clazz);

                if ( bytes != null ) {
                    list.add( new ClassBytes ( clazz, bytes ) );
                }
                
            }

            response.setClasses(list);

            output.writeObject(response);

        } else if ( message instanceof ExitProgramRequest ) {

            // woohoo work's over

            ExitProgramResponse response = new ExitProgramResponse();

            output.writeObject(response);
            output.flush();

            output.close();
            
            System.exit(0);

        } else if ( message instanceof StartCanaryRequest ) {

            StartCanaryRequest request = (StartCanaryRequest)message;
            StartCanaryResponse response = new StartCanaryResponse();

            try {

                AgentToSnoopClient.currentClient().setCanary(request.getCanary());
                CanaryUtil.applyCanaries(manager, request.getType(), request.getCanary(), request.getPackage());

            //} catch (InstrumentationException ex) {
            } catch (Throwable t) {
                populateResponse(response, t);
            }

            output.writeObject(response);
            output.flush();

            output.close();

        } else if ( message instanceof StopCanaryRequest ) {

            StopCanaryResponse response = new StopCanaryResponse();

            try {

                manager.resetAllClasses();
                AgentToSnoopClient.currentClient().clearCanary();

            } catch (InstrumentationException ex) {
                populateResponse(response, ex);
            }

            output.writeObject(response);
            output.flush();

            output.close();


        } else {

            UnrecognizedMessage response = new UnrecognizedMessage();

            output.writeObject(response);

        }

    }

    protected int getServerPort() {
        return ourPort;
    }

    protected int getHomePort() {
        return homePort;
    }

    /**
     * This function is the entry point into the instrumentation of the
     * running program. It turns on the rules we instrument.
     */
    private void uninstallHooks() throws InstrumentationException {

        if ( snoopSession == null ) {
            return;
        }

        for (FunctionHookInterceptor hook : snoopSession.getFunctionHooks()) {
            
            try {

                Class clazz = manager.getFromAllClasses(hook.getClassName());

                if ( ! manager.hasClassBeenModified(clazz) ) {
                    continue;
                }

                if ( ReflectionUtil.isInterfaceOrAbstract(clazz) || hook.isAppliedToSubtypes() ) {

                    Class[] subtypes = getAllSubtypes(clazz);

                    for (Class c : subtypes ) {
                        manager.deinstrument(c);
                    }

                } 

                try {
                    manager.deinstrument(clazz);
                } catch(InstrumentationException e) {
                    // this could be normal, but its faster to just catch-ignore
                }

                
            } catch (ClassNotFoundException cnfe) {
                throw new InstrumentationException(cnfe);
            }
            
        }
    }

    private void installHooks() throws InstrumentationException {
    
        HashMap<Class, ClassChanges> classChanges = new HashMap<Class,ClassChanges>();

        for(FunctionHookInterceptor hook : snoopSession.getFunctionHooks() ) {

            try {

                Class clazz = manager.getFromAllClasses(hook.getClassName());

                // if it applies to all subtypes, only do it to the subtypes
                if ( ReflectionUtil.isInterfaceOrAbstract(clazz) || hook.isAppliedToSubtypes() ) {
                    Class[] subtypes = getAllSubtypes(clazz);

                    for (Class c : subtypes ) {
                        ClassChanges change = classChanges.get(c);

                        if (change == null) {
                            change = new ClassChanges(c);
                            classChanges.put(c, change);
                        }

                        change.registerHook(hook, manager);
                    }

                }

                if ( ! ReflectionUtil.isInterfaceOrAbstract(clazz) ) {
                    ClassChanges change = classChanges.get(clazz);

                    if (change == null) {
                        change = new ClassChanges(clazz);
                        classChanges.put(clazz, change);
                    }

                    change.registerHook(hook, manager);
                }
                
            } catch (ClassNotFoundException ex) {
                throw new InstrumentationException(ex);
            }

        }

        for ( Class clazz : classChanges.keySet() ) {

            try {

                ClassChanges change = classChanges.get(clazz);
                
                manager.instrument(
                        clazz,
                        change.getAllMethodChanges());

            } catch (InstrumentationException ex) {
                throw ex;
            }

        }
    }

    private Class[] getAllSubtypes(Class clazz) {
        List<Class> subtypes = new ArrayList<Class>();

        for(Class c : manager.getLoadedClasses() ) {
            if ( clazz.isAssignableFrom(c) && ! c.isInterface() && ! c.equals(clazz)) {
                subtypes.add(c);
            }
        }

        return subtypes.toArray( new Class[]{} );
    }

}