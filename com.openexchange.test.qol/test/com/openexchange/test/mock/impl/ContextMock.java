
package com.openexchange.test.mock.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.mockito.Mockito;
import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.groupware.contexts.Context;

@NonNullByDefault
public class ContextMock {

    public static ContextMock builder() {
        return new ContextMock();
    }

    private Context context;

    public Context build() {
        return context;
    }

    private final Map<String, List<String>> attributes = new ConcurrentHashMap<String, List<String>>();

    private ContextMock() {
        context = Mockito.mock(Context.class);
        Mockito.when(context.getAttributes()).thenReturn(attributes);
    }

    public ContextMock withContextId(int id) {
        Mockito.when(context.getContextId()).thenReturn(id);
        return this;
    }

    public ContextMock withFileStorageAuth(String[] fileStorageAuth) {
        Mockito.when(context.getFileStorageAuth()).thenReturn(fileStorageAuth);
        return this;
    }

    public ContextMock withFileStorageQuota(long fileStorageQuota) {
        Mockito.when(context.getFileStorageQuota()).thenReturn(fileStorageQuota);
        return this;
    }

    public ContextMock withFilestoreId(int filestoreId) {
        Mockito.when(context.getFilestoreId()).thenReturn(filestoreId);
        return this;
    }

    public ContextMock withFilestoreName(String filestoreName) {
        Mockito.when(context.getFilestoreName()).thenReturn(filestoreName);
        return this;
    }

    public ContextMock withLoginInfo(String[] loginInfo) {
        Mockito.when(context.getLoginInfo()).thenReturn(loginInfo);
        return this;
    }

    public ContextMock withMailadmin(int mailadmin) {
        Mockito.when(context.getMailadmin()).thenReturn(mailadmin);
        return this;
    }

    public ContextMock withName(String name) {
        Mockito.when(context.getName()).thenReturn(name);
        return this;
    }

    public ContextMock setEnabled(boolean isEnabled) {
        Mockito.when(context.isEnabled()).thenReturn(isEnabled);
        return this;
    }

    public ContextMock setUpdating(boolean isUpdating) {
        Mockito.when(context.isUpdating()).thenReturn(isUpdating);
        return this;
    }

    public ContextMock setReadOnly(boolean isReadOnly) {
        Mockito.when(context.isReadOnly()).thenReturn(isReadOnly);
        return this;
    }

    public ContextMock setAttribute(String key, String value) {
        attributes.put(key, Collections.singletonList(value));
        return this;
    }

}
