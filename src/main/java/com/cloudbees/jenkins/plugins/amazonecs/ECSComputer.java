/*
 * The MIT License
 *
 *  Copyright (c) 2015, CloudBees, Inc.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 */

package com.cloudbees.jenkins.plugins.amazonecs;

import hudson.model.Executor;
import hudson.model.Queue;
import hudson.slaves.AbstractCloudComputer;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Amazon EC2 Container Service implementation of {@link hudson.model.Computer}
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ECSComputer extends AbstractCloudComputer<ECSSlave> {
    private static final Logger LOGGER = Logger.getLogger(ECSComputer.class.getName());

    private volatile boolean isAcceptingTasks = true;

    public ECSComputer(ECSSlave slave) {
        super(slave);
    }

    @Override
    public void taskCompleted(Executor executor, Queue.Task task, long durationMS) {
        try {
            super.taskCompleted(executor, task, durationMS);
        } finally {
            terminate();
        }
    }

    @Override
    public void taskCompletedWithProblems(Executor executor, Queue.Task task, long durationMS, Throwable problems) {
        try {
            super.taskCompletedWithProblems(executor, task, durationMS, problems);
        } finally {
            terminate();
        }
    }

    @Override
    public boolean isAcceptingTasks() {
        return isAcceptingTasks && super.isAcceptingTasks();
    }

    /**
     * Computer is terminated after build completion so we enforce it will only be used once.
     */
    private void terminate() {
        isAcceptingTasks = false;

        threadPoolForRemoting.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(20000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }

                try {
                    getNode().terminate();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error while terminating ECS task: ", e);
                }
            }
        });
    }
}
