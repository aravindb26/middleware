
package com.openexchange.admin.soap.taskmgmt.soap;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 2.6.0
 * 2012-06-01T19:02:36.622+02:00
 * Generated source version: 2.6.0
 */

@WebFault(name = "TaskManagerException", targetNamespace = "http://soap.admin.openexchange.com")
public class TaskManagerException_Exception extends java.lang.Exception {

    private com.openexchange.admin.soap.taskmgmt.soap.TaskManagerException taskManagerException;

    public TaskManagerException_Exception() {
        super();
    }

    public TaskManagerException_Exception(String message) {
        super(message);
    }

    public TaskManagerException_Exception(String message, Throwable cause) {
        super(message, cause);
    }

    public TaskManagerException_Exception(String message, com.openexchange.admin.soap.taskmgmt.soap.TaskManagerException taskManagerException) {
        super(message);
        this.taskManagerException = taskManagerException;
    }

    public TaskManagerException_Exception(String message, com.openexchange.admin.soap.taskmgmt.soap.TaskManagerException taskManagerException, Throwable cause) {
        super(message, cause);
        this.taskManagerException = taskManagerException;
    }

    public com.openexchange.admin.soap.taskmgmt.soap.TaskManagerException getFaultInfo() {
        return this.taskManagerException;
    }
}