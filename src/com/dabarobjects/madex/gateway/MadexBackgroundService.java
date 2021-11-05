/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway;

import com.dabarobjects.activity.plug.AbstractSpikeActivityPlug; 
import com.dabarobjects.activity.plug.SpikeActivityServerPlugExecutorModel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 *
 * @author dabar
 */
public class MadexBackgroundService implements ServletContextListener, HttpSessionListener {

    private static final MadexBackgroundService SERVICE = new MadexBackgroundService();

    public static MadexBackgroundService get() {
        return SERVICE;
    }
    private ExecutorService executor;

    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("Commencing Thread Executors....");
        SERVICE.executor = Executors.newCachedThreadPool();
        
        SpikeActivityServerPlugExecutorModel.getInstance().setSystemThreadService(SERVICE.executor);
        //SERVICE.addBackgroundTask(new SumStateDataPlug());
       


    }

    public void contextDestroyed(ServletContextEvent sce) {
        if (SERVICE.executor != null) {
            System.out.println("Shutting Down Thread Executors...");
            SERVICE.executor.shutdownNow();

        }
    }

    public void addBackgroundTask(AbstractSpikeActivityPlug task) {
        task.setExecutorService(executor);
        SpikeActivityServerPlugExecutorModel.getInstance().addEventPlugAtTop(task);
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
    }
}
