package com.openexchange.ajax.oauth.provider.protocol;

import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.folderstorage.ContentType;
import com.openexchange.session.restricted.Scope;

public class TestData {

    private Scope scope;
    private ContentType contentType;
    private EnumAPI api;
    private boolean altNames;

    public TestData(Scope scope, ContentType contentType, EnumAPI api, boolean altNames){
        this.scope=scope;
        this.contentType=contentType;
        this.api=api;
        this.altNames=altNames;
    }

    public Scope getScope(){
        return scope;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public EnumAPI getApi(){
        return api;
    }

    public boolean getAltNames(){
        return altNames;
    }
}
