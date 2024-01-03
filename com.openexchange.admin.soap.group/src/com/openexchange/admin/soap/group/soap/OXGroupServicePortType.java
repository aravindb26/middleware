package com.openexchange.admin.soap.group.soap;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.Action;
import javax.xml.ws.FaultAction;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

/**
 * This class was generated by Apache CXF 2.6.0
 * 2012-06-01T18:50:57.336+02:00
 * Generated source version: 2.6.0
 *
 */
@WebService(targetNamespace = "http://soap.admin.openexchange.com", name = "OXGroupServicePortType")
@XmlSeeAlso({com.openexchange.admin.soap.group.exceptions.ObjectFactory.class, com.openexchange.admin.soap.group.dataobjects.ObjectFactory.class, com.openexchange.admin.soap.group.io.ObjectFactory.class, ObjectFactory.class, com.openexchange.admin.soap.group.rmi.ObjectFactory.class})
public interface OXGroupServicePortType {

    @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
    @Action(input = "urn:change", output = "urn:changeResponse", fault = {@FaultAction(className = StorageException_Exception.class, value = "urn:changeStorageException"), @FaultAction(className = InvalidCredentialsException_Exception.class, value = "urn:changeInvalidCredentialsException"), @FaultAction(className = InvalidDataException_Exception.class, value = "urn:changeInvalidDataException"), @FaultAction(className = NoSuchGroupException_Exception.class, value = "urn:changeNoSuchGroupException"), @FaultAction(className = NoSuchContextException_Exception.class, value = "urn:changeNoSuchContextException"), @FaultAction(className = RemoteException_Exception.class, value = "urn:changeRemoteException"), @FaultAction(className = NoSuchUserException_Exception.class, value = "urn:changeNoSuchUserException"), @FaultAction(className = DatabaseUpdateException_Exception.class, value = "urn:changeDatabaseUpdateException")})
    @WebMethod(action = "urn:change")
    public void change(
        @WebParam(partName = "parameters", name = "change", targetNamespace = "http://soap.admin.openexchange.com")
        Change parameters
    ) throws StorageException_Exception, InvalidCredentialsException_Exception, InvalidDataException_Exception, NoSuchGroupException_Exception, NoSuchContextException_Exception, RemoteException_Exception, NoSuchUserException_Exception, DatabaseUpdateException_Exception;

    @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
    @Action(input = "urn:removeMember", output = "urn:removeMemberResponse", fault = {@FaultAction(className = StorageException_Exception.class, value = "urn:removeMemberStorageException"), @FaultAction(className = InvalidCredentialsException_Exception.class, value = "urn:removeMemberInvalidCredentialsException"), @FaultAction(className = InvalidDataException_Exception.class, value = "urn:removeMemberInvalidDataException"), @FaultAction(className = NoSuchGroupException_Exception.class, value = "urn:removeMemberNoSuchGroupException"), @FaultAction(className = NoSuchContextException_Exception.class, value = "urn:removeMemberNoSuchContextException"), @FaultAction(className = RemoteException_Exception.class, value = "urn:removeMemberRemoteException"), @FaultAction(className = NoSuchUserException_Exception.class, value = "urn:removeMemberNoSuchUserException"), @FaultAction(className = DatabaseUpdateException_Exception.class, value = "urn:removeMemberDatabaseUpdateException")})
    @WebMethod(action = "urn:removeMember")
    public void removeMember(
        @WebParam(partName = "parameters", name = "removeMember", targetNamespace = "http://soap.admin.openexchange.com")
        RemoveMember parameters
    ) throws StorageException_Exception, InvalidCredentialsException_Exception, InvalidDataException_Exception, NoSuchGroupException_Exception, NoSuchContextException_Exception, RemoteException_Exception, NoSuchUserException_Exception, DatabaseUpdateException_Exception;

    @WebResult(name = "return", targetNamespace = "http://soap.admin.openexchange.com")
    @Action(input = "urn:listAll", output = "urn:listAllResponse", fault = {@FaultAction(className = StorageException_Exception.class, value = "urn:listAllStorageException"), @FaultAction(className = InvalidCredentialsException_Exception.class, value = "urn:listAllInvalidCredentialsException"), @FaultAction(className = InvalidDataException_Exception.class, value = "urn:listAllInvalidDataException"), @FaultAction(className = NoSuchContextException_Exception.class, value = "urn:listAllNoSuchContextException"), @FaultAction(className = RemoteException_Exception.class, value = "urn:listAllRemoteException"), @FaultAction(className = DatabaseUpdateException_Exception.class, value = "urn:listAllDatabaseUpdateException")})
    @RequestWrapper(localName = "listAll", targetNamespace = "http://soap.admin.openexchange.com", className = "com.openexchange.admin.soap.group.soap.ListAll")
    @WebMethod(action = "urn:listAll")
    @ResponseWrapper(localName = "listAllResponse", targetNamespace = "http://soap.admin.openexchange.com", className = "com.openexchange.admin.soap.group.soap.ListAllResponse")
    public java.util.List<com.openexchange.admin.soap.group.dataobjects.Group> listAll(
        @WebParam(name = "ctx", targetNamespace = "http://soap.admin.openexchange.com")
        com.openexchange.admin.soap.group.dataobjects.Context ctx,
        @WebParam(name = "auth", targetNamespace = "http://soap.admin.openexchange.com")
        com.openexchange.admin.soap.group.dataobjects.Credentials auth
    ) throws StorageException_Exception, InvalidCredentialsException_Exception, InvalidDataException_Exception, NoSuchContextException_Exception, RemoteException_Exception, DatabaseUpdateException_Exception;

    @WebResult(name = "return", targetNamespace = "http://soap.admin.openexchange.com")
    @Action(input = "urn:getDefaultGroup", output = "urn:getDefaultGroupResponse", fault = {@FaultAction(className = StorageException_Exception.class, value = "urn:getDefaultGroupStorageException"), @FaultAction(className = InvalidCredentialsException_Exception.class, value = "urn:getDefaultGroupInvalidCredentialsException"), @FaultAction(className = InvalidDataException_Exception.class, value = "urn:getDefaultGroupInvalidDataException"), @FaultAction(className = NoSuchContextException_Exception.class, value = "urn:getDefaultGroupNoSuchContextException"), @FaultAction(className = RemoteException_Exception.class, value = "urn:getDefaultGroupRemoteException"), @FaultAction(className = DatabaseUpdateException_Exception.class, value = "urn:getDefaultGroupDatabaseUpdateException")})
    @RequestWrapper(localName = "getDefaultGroup", targetNamespace = "http://soap.admin.openexchange.com", className = "com.openexchange.admin.soap.group.soap.GetDefaultGroup")
    @WebMethod(action = "urn:getDefaultGroup")
    @ResponseWrapper(localName = "getDefaultGroupResponse", targetNamespace = "http://soap.admin.openexchange.com", className = "com.openexchange.admin.soap.group.soap.GetDefaultGroupResponse")
    public com.openexchange.admin.soap.group.dataobjects.Group getDefaultGroup(
        @WebParam(name = "ctx", targetNamespace = "http://soap.admin.openexchange.com")
        com.openexchange.admin.soap.group.dataobjects.Context ctx,
        @WebParam(name = "auth", targetNamespace = "http://soap.admin.openexchange.com")
        com.openexchange.admin.soap.group.dataobjects.Credentials auth
    ) throws StorageException_Exception, InvalidCredentialsException_Exception, InvalidDataException_Exception, NoSuchContextException_Exception, RemoteException_Exception, DatabaseUpdateException_Exception;

    @WebResult(name = "return", targetNamespace = "http://soap.admin.openexchange.com")
    @Action(input = "urn:getMembers", output = "urn:getMembersResponse", fault = {@FaultAction(className = StorageException_Exception.class, value = "urn:getMembersStorageException"), @FaultAction(className = InvalidCredentialsException_Exception.class, value = "urn:getMembersInvalidCredentialsException"), @FaultAction(className = InvalidDataException_Exception.class, value = "urn:getMembersInvalidDataException"), @FaultAction(className = NoSuchGroupException_Exception.class, value = "urn:getMembersNoSuchGroupException"), @FaultAction(className = NoSuchContextException_Exception.class, value = "urn:getMembersNoSuchContextException"), @FaultAction(className = RemoteException_Exception.class, value = "urn:getMembersRemoteException"), @FaultAction(className = DatabaseUpdateException_Exception.class, value = "urn:getMembersDatabaseUpdateException")})
    @RequestWrapper(localName = "getMembers", targetNamespace = "http://soap.admin.openexchange.com", className = "com.openexchange.admin.soap.group.soap.GetMembers")
    @WebMethod(action = "urn:getMembers")
    @ResponseWrapper(localName = "getMembersResponse", targetNamespace = "http://soap.admin.openexchange.com", className = "com.openexchange.admin.soap.group.soap.GetMembersResponse")
    public java.util.List<com.openexchange.admin.soap.group.dataobjects.User> getMembers(
        @WebParam(name = "ctx", targetNamespace = "http://soap.admin.openexchange.com")
        com.openexchange.admin.soap.group.dataobjects.Context ctx,
        @WebParam(name = "grp", targetNamespace = "http://soap.admin.openexchange.com")
        com.openexchange.admin.soap.group.dataobjects.Group grp,
        @WebParam(name = "auth", targetNamespace = "http://soap.admin.openexchange.com")
        com.openexchange.admin.soap.group.dataobjects.Credentials auth
    ) throws StorageException_Exception, InvalidCredentialsException_Exception, InvalidDataException_Exception, NoSuchGroupException_Exception, NoSuchContextException_Exception, RemoteException_Exception, DatabaseUpdateException_Exception;

    @WebResult(name = "return", targetNamespace = "http://soap.admin.openexchange.com")
    @Action(input = "urn:listGroupsForUser", output = "urn:listGroupsForUserResponse", fault = {@FaultAction(className = StorageException_Exception.class, value = "urn:listGroupsForUserStorageException"), @FaultAction(className = InvalidCredentialsException_Exception.class, value = "urn:listGroupsForUserInvalidCredentialsException"), @FaultAction(className = InvalidDataException_Exception.class, value = "urn:listGroupsForUserInvalidDataException"), @FaultAction(className = NoSuchContextException_Exception.class, value = "urn:listGroupsForUserNoSuchContextException"), @FaultAction(className = RemoteException_Exception.class, value = "urn:listGroupsForUserRemoteException"), @FaultAction(className = NoSuchUserException_Exception.class, value = "urn:listGroupsForUserNoSuchUserException"), @FaultAction(className = DatabaseUpdateException_Exception.class, value = "urn:listGroupsForUserDatabaseUpdateException")})
    @RequestWrapper(localName = "listGroupsForUser", targetNamespace = "http://soap.admin.openexchange.com", className = "com.openexchange.admin.soap.group.soap.ListGroupsForUser")
    @WebMethod(action = "urn:listGroupsForUser")
    @ResponseWrapper(localName = "listGroupsForUserResponse", targetNamespace = "http://soap.admin.openexchange.com", className = "com.openexchange.admin.soap.group.soap.ListGroupsForUserResponse")
    public java.util.List<com.openexchange.admin.soap.group.dataobjects.Group> listGroupsForUser(
        @WebParam(name = "ctx", targetNamespace = "http://soap.admin.openexchange.com")
        com.openexchange.admin.soap.group.dataobjects.Context ctx,
        @WebParam(name = "usr", targetNamespace = "http://soap.admin.openexchange.com")
        com.openexchange.admin.soap.group.dataobjects.User usr,
        @WebParam(name = "auth", targetNamespace = "http://soap.admin.openexchange.com")
        com.openexchange.admin.soap.group.dataobjects.Credentials auth
    ) throws StorageException_Exception, InvalidCredentialsException_Exception, InvalidDataException_Exception, NoSuchContextException_Exception, RemoteException_Exception, NoSuchUserException_Exception, DatabaseUpdateException_Exception;

    @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
    @Action(input = "urn:delete", output = "urn:deleteResponse", fault = {@FaultAction(className = StorageException_Exception.class, value = "urn:deleteStorageException"), @FaultAction(className = InvalidCredentialsException_Exception.class, value = "urn:deleteInvalidCredentialsException"), @FaultAction(className = InvalidDataException_Exception.class, value = "urn:deleteInvalidDataException"), @FaultAction(className = NoSuchGroupException_Exception.class, value = "urn:deleteNoSuchGroupException"), @FaultAction(className = NoSuchContextException_Exception.class, value = "urn:deleteNoSuchContextException"), @FaultAction(className = RemoteException_Exception.class, value = "urn:deleteRemoteException"), @FaultAction(className = DatabaseUpdateException_Exception.class, value = "urn:deleteDatabaseUpdateException")})
    @WebMethod(action = "urn:delete")
    public void delete(
        @WebParam(partName = "parameters", name = "delete", targetNamespace = "http://soap.admin.openexchange.com")
        Delete parameters
    ) throws StorageException_Exception, InvalidCredentialsException_Exception, InvalidDataException_Exception, NoSuchGroupException_Exception, NoSuchContextException_Exception, RemoteException_Exception, DatabaseUpdateException_Exception;

    @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
    @Action(input = "urn:deleteMultiple", output = "urn:deleteMultipleResponse", fault = {@FaultAction(className = StorageException_Exception.class, value = "urn:deleteMultipleStorageException"), @FaultAction(className = InvalidCredentialsException_Exception.class, value = "urn:deleteMultipleInvalidCredentialsException"), @FaultAction(className = InvalidDataException_Exception.class, value = "urn:deleteMultipleInvalidDataException"), @FaultAction(className = NoSuchGroupException_Exception.class, value = "urn:deleteMultipleNoSuchGroupException"), @FaultAction(className = NoSuchContextException_Exception.class, value = "urn:deleteMultipleNoSuchContextException"), @FaultAction(className = RemoteException_Exception.class, value = "urn:deleteMultipleRemoteException"), @FaultAction(className = DatabaseUpdateException_Exception.class, value = "urn:deleteMultipleDatabaseUpdateException")})
    @WebMethod(action = "urn:deleteMultiple")
    public void deleteMultiple(
        @WebParam(partName = "parameters", name = "deleteMultiple", targetNamespace = "http://soap.admin.openexchange.com")
        DeleteMultiple parameters
    ) throws StorageException_Exception, InvalidCredentialsException_Exception, InvalidDataException_Exception, NoSuchGroupException_Exception, NoSuchContextException_Exception, RemoteException_Exception, DatabaseUpdateException_Exception;

    @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
    @Action(input = "urn:addMember", output = "urn:addMemberResponse", fault = {@FaultAction(className = StorageException_Exception.class, value = "urn:addMemberStorageException"), @FaultAction(className = InvalidCredentialsException_Exception.class, value = "urn:addMemberInvalidCredentialsException"), @FaultAction(className = InvalidDataException_Exception.class, value = "urn:addMemberInvalidDataException"), @FaultAction(className = NoSuchGroupException_Exception.class, value = "urn:addMemberNoSuchGroupException"), @FaultAction(className = NoSuchContextException_Exception.class, value = "urn:addMemberNoSuchContextException"), @FaultAction(className = RemoteException_Exception.class, value = "urn:addMemberRemoteException"), @FaultAction(className = NoSuchUserException_Exception.class, value = "urn:addMemberNoSuchUserException"), @FaultAction(className = DatabaseUpdateException_Exception.class, value = "urn:addMemberDatabaseUpdateException")})
    @WebMethod(action = "urn:addMember")
    public void addMember(
        @WebParam(partName = "parameters", name = "addMember", targetNamespace = "http://soap.admin.openexchange.com")
        AddMember parameters
    ) throws StorageException_Exception, InvalidCredentialsException_Exception, InvalidDataException_Exception, NoSuchGroupException_Exception, NoSuchContextException_Exception, RemoteException_Exception, NoSuchUserException_Exception, DatabaseUpdateException_Exception;

    @WebResult(name = "return", targetNamespace = "http://soap.admin.openexchange.com")
    @Action(input = "urn:getMultipleData", output = "urn:getMultipleDataResponse", fault = {@FaultAction(className = StorageException_Exception.class, value = "urn:getMultipleDataStorageException"), @FaultAction(className = InvalidCredentialsException_Exception.class, value = "urn:getMultipleDataInvalidCredentialsException"), @FaultAction(className = InvalidDataException_Exception.class, value = "urn:getMultipleDataInvalidDataException"), @FaultAction(className = NoSuchGroupException_Exception.class, value = "urn:getMultipleDataNoSuchGroupException"), @FaultAction(className = NoSuchContextException_Exception.class, value = "urn:getMultipleDataNoSuchContextException"), @FaultAction(className = RemoteException_Exception.class, value = "urn:getMultipleDataRemoteException"), @FaultAction(className = DatabaseUpdateException_Exception.class, value = "urn:getMultipleDataDatabaseUpdateException")})
    @RequestWrapper(localName = "getMultipleData", targetNamespace = "http://soap.admin.openexchange.com", className = "com.openexchange.admin.soap.group.soap.GetMultipleData")
    @WebMethod(action = "urn:getMultipleData")
    @ResponseWrapper(localName = "getMultipleDataResponse", targetNamespace = "http://soap.admin.openexchange.com", className = "com.openexchange.admin.soap.group.soap.GetMultipleDataResponse")
    public java.util.List<com.openexchange.admin.soap.group.dataobjects.Group> getMultipleData(
        @WebParam(name = "ctx", targetNamespace = "http://soap.admin.openexchange.com")
        com.openexchange.admin.soap.group.dataobjects.Context ctx,
        @WebParam(name = "grps", targetNamespace = "http://soap.admin.openexchange.com")
        java.util.List<com.openexchange.admin.soap.group.dataobjects.Group> grps,
        @WebParam(name = "auth", targetNamespace = "http://soap.admin.openexchange.com")
        com.openexchange.admin.soap.group.dataobjects.Credentials auth
    ) throws StorageException_Exception, InvalidCredentialsException_Exception, InvalidDataException_Exception, NoSuchGroupException_Exception, NoSuchContextException_Exception, RemoteException_Exception, DatabaseUpdateException_Exception;

    @WebResult(name = "return", targetNamespace = "http://soap.admin.openexchange.com")
    @Action(input = "urn:create", output = "urn:createResponse", fault = {@FaultAction(className = StorageException_Exception.class, value = "urn:createStorageException"), @FaultAction(className = InvalidCredentialsException_Exception.class, value = "urn:createInvalidCredentialsException"), @FaultAction(className = InvalidDataException_Exception.class, value = "urn:createInvalidDataException"), @FaultAction(className = NoSuchContextException_Exception.class, value = "urn:createNoSuchContextException"), @FaultAction(className = RemoteException_Exception.class, value = "urn:createRemoteException"), @FaultAction(className = NoSuchUserException_Exception.class, value = "urn:createNoSuchUserException"), @FaultAction(className = DatabaseUpdateException_Exception.class, value = "urn:createDatabaseUpdateException")})
    @RequestWrapper(localName = "create", targetNamespace = "http://soap.admin.openexchange.com", className = "com.openexchange.admin.soap.group.soap.Create")
    @WebMethod(action = "urn:create")
    @ResponseWrapper(localName = "createResponse", targetNamespace = "http://soap.admin.openexchange.com", className = "com.openexchange.admin.soap.group.soap.CreateResponse")
    public com.openexchange.admin.soap.group.dataobjects.Group create(
        @WebParam(name = "ctx", targetNamespace = "http://soap.admin.openexchange.com")
        com.openexchange.admin.soap.group.dataobjects.Context ctx,
        @WebParam(name = "grp", targetNamespace = "http://soap.admin.openexchange.com")
        com.openexchange.admin.soap.group.dataobjects.Group grp,
        @WebParam(name = "auth", targetNamespace = "http://soap.admin.openexchange.com")
        com.openexchange.admin.soap.group.dataobjects.Credentials auth
    ) throws StorageException_Exception, InvalidCredentialsException_Exception, InvalidDataException_Exception, NoSuchContextException_Exception, RemoteException_Exception, NoSuchUserException_Exception, DatabaseUpdateException_Exception;

    @WebResult(name = "return", targetNamespace = "http://soap.admin.openexchange.com")
    @Action(input = "urn:list", output = "urn:listResponse", fault = {@FaultAction(className = StorageException_Exception.class, value = "urn:listStorageException"), @FaultAction(className = InvalidCredentialsException_Exception.class, value = "urn:listInvalidCredentialsException"), @FaultAction(className = InvalidDataException_Exception.class, value = "urn:listInvalidDataException"), @FaultAction(className = NoSuchContextException_Exception.class, value = "urn:listNoSuchContextException"), @FaultAction(className = RemoteException_Exception.class, value = "urn:listRemoteException"), @FaultAction(className = DatabaseUpdateException_Exception.class, value = "urn:listDatabaseUpdateException")})
    @RequestWrapper(localName = "list", targetNamespace = "http://soap.admin.openexchange.com", className = "com.openexchange.admin.soap.group.soap.List")
    @WebMethod(action = "urn:list")
    @ResponseWrapper(localName = "listResponse", targetNamespace = "http://soap.admin.openexchange.com", className = "com.openexchange.admin.soap.group.soap.ListResponse")
    public java.util.List<com.openexchange.admin.soap.group.dataobjects.Group> list(
        @WebParam(name = "ctx", targetNamespace = "http://soap.admin.openexchange.com")
        com.openexchange.admin.soap.group.dataobjects.Context ctx,
        @WebParam(name = "pattern", targetNamespace = "http://soap.admin.openexchange.com")
        java.lang.String pattern,
        @WebParam(name = "auth", targetNamespace = "http://soap.admin.openexchange.com")
        com.openexchange.admin.soap.group.dataobjects.Credentials auth
    ) throws StorageException_Exception, InvalidCredentialsException_Exception, InvalidDataException_Exception, NoSuchContextException_Exception, RemoteException_Exception, DatabaseUpdateException_Exception;

    @WebResult(name = "return", targetNamespace = "http://soap.admin.openexchange.com")
    @Action(input = "urn:getData", output = "urn:getDataResponse", fault = {@FaultAction(className = StorageException_Exception.class, value = "urn:getDataStorageException"), @FaultAction(className = InvalidCredentialsException_Exception.class, value = "urn:getDataInvalidCredentialsException"), @FaultAction(className = InvalidDataException_Exception.class, value = "urn:getDataInvalidDataException"), @FaultAction(className = NoSuchGroupException_Exception.class, value = "urn:getDataNoSuchGroupException"), @FaultAction(className = NoSuchContextException_Exception.class, value = "urn:getDataNoSuchContextException"), @FaultAction(className = RemoteException_Exception.class, value = "urn:getDataRemoteException"), @FaultAction(className = DatabaseUpdateException_Exception.class, value = "urn:getDataDatabaseUpdateException")})
    @RequestWrapper(localName = "getData", targetNamespace = "http://soap.admin.openexchange.com", className = "com.openexchange.admin.soap.group.soap.GetData")
    @WebMethod(action = "urn:getData")
    @ResponseWrapper(localName = "getDataResponse", targetNamespace = "http://soap.admin.openexchange.com", className = "com.openexchange.admin.soap.group.soap.GetDataResponse")
    public com.openexchange.admin.soap.group.dataobjects.Group getData(
        @WebParam(name = "ctx", targetNamespace = "http://soap.admin.openexchange.com")
        com.openexchange.admin.soap.group.dataobjects.Context ctx,
        @WebParam(name = "grp", targetNamespace = "http://soap.admin.openexchange.com")
        com.openexchange.admin.soap.group.dataobjects.Group grp,
        @WebParam(name = "auth", targetNamespace = "http://soap.admin.openexchange.com")
        com.openexchange.admin.soap.group.dataobjects.Credentials auth
    ) throws StorageException_Exception, InvalidCredentialsException_Exception, InvalidDataException_Exception, NoSuchGroupException_Exception, NoSuchContextException_Exception, RemoteException_Exception, DatabaseUpdateException_Exception;
}
