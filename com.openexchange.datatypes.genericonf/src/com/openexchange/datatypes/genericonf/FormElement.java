/*
 * @copyright Copyright (c) OX Software GmbH, Germany <info@open-xchange.com>
 * @license AGPL-3.0
 *
 * This code is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OX App Suite.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>.
 *
 * Any use of the work other than as authorized under this license or copyright law is prohibited.
 *
 */

package com.openexchange.datatypes.genericonf;

import java.util.HashMap;
import java.util.Map;
import org.w3c.dom.Element;


/**
 * {@link FormElement}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class FormElement {
    // Known Custom Widgets:  oauthAccount
    public static enum Widget {
        INPUT("input"), PASSWORD("password"), CHECKBOX("checkbox"), LINK("link"), TEXT("text"), CUSTOM("custom");

        private final String keyword;


        private Widget(String keyword) {
            this.keyword = keyword;
        }

        public String getKeyword() {
            return keyword;
        }

        public Object doSwitch(WidgetSwitcher switcher, Object... args) {
            switch (this) {
            case INPUT:
                return switcher.input(args);
            case PASSWORD:
                return switcher.password(args);
            case CHECKBOX:
                return switcher.checkbox(args);
            case LINK:
                return switcher.link(args);
            case TEXT:
                return switcher.text(args);
            case CUSTOM:
                return switcher.custom(args);
            default:
                throw new IllegalArgumentException("Didn't understand "+this.getKeyword());
            }
        }

        public static Widget getWidgetByKeyword(String keyword) {
            for(Widget widget : values()) {
                if (widget.getKeyword().equals(keyword)) {
                    return widget;
                }
            }
            return null;
        }

        public static Widget chooseFromHTMLElement(Element inputElement) {
            if ("input".equalsIgnoreCase(inputElement.getTagName())) {
                String type = inputElement.getAttribute("type");
                if ("text".equalsIgnoreCase(type)) {
                    return INPUT;
                } else if ("password".equalsIgnoreCase(type)) {
                    return PASSWORD;
                } else if ("checkbox".equalsIgnoreCase(type)) {
                    return CHECKBOX;
                }
            }
            return null;
        }
    }

    private String name;

    private String displayName;

    private Widget widget;

    private String customWidget;

    private Object defaultValue;

    private boolean mandatory;

    private final Map<String, String> options = new HashMap<String, String>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Widget getWidget() {
        return widget;
    }

    public String getCustomWidget() {
        return customWidget;
    }

    public void setCustomWidget(String customWidget) {
        this.customWidget = customWidget;
    }

    public void setWidget(Widget widget) {
        this.widget = widget;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public FormElement setOption(String optionName, String value) {
        options.put(optionName, value);
        return this;
    }

    public FormElement removeOption(String optionName) {
        options.remove(optionName);
        return this;
    }

    public FormElement clearOptions() {
        options.clear();
        return this;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    @Override
    public String toString() {
        return name;
    }

    public static FormElement formElement(String name, String displayName, boolean mandatory, Object defaultValue) {
        FormElement formElement = new FormElement();
        formElement.setName(name);
        formElement.setDisplayName(displayName);
        formElement.setMandatory(mandatory);
        formElement.setDefaultValue(defaultValue);
        return formElement;
    }

    public static FormElement input(String name, String displayName, boolean mandatory, String defaultValue) {
        FormElement formElement = formElement(name, displayName, mandatory, defaultValue);
        formElement.setWidget(Widget.INPUT);
        return formElement;
    }

    public static FormElement input(String name, String displayName) {
        return input(name, displayName, true, null);
    }

    public static FormElement password(String name, String displayName, boolean mandatory, String defaultValue) {
        FormElement formElement = formElement(name, displayName, mandatory, defaultValue);
        formElement.setWidget(Widget.PASSWORD);
        return formElement;
    }
    public static FormElement link(String name, String displayName, boolean mandatory, Boolean defaultValue) {
        FormElement formElement = formElement(name, displayName, mandatory, defaultValue);
        formElement.setWidget(Widget.LINK);
        return formElement;
    }
    public static FormElement link(String name, String displayName) {
        return link(name, displayName, true, null);
    }

    public static FormElement password(String name, String displayName) {
        return password(name, displayName, true, null);
    }

    public static FormElement text(String name, String displayName, boolean mandatory, Boolean defaultValue) {
        FormElement formElement = formElement(name, displayName, mandatory, defaultValue);
        formElement.setWidget(Widget.TEXT);
        return formElement;
    }
    public static FormElement text(String name, String displayName) {
        return text(name, displayName, true, null);
    }

    public static FormElement checkbox(String name, String displayName, boolean mandatory, Boolean defaultValue) {
        FormElement formElement = formElement(name, displayName, mandatory, defaultValue);
        formElement.setWidget(Widget.CHECKBOX);
        return formElement;
    }
    public static FormElement checkbox(String name, String displayName) {
        return checkbox(name, displayName, true, null);
    }

    public static FormElement custom(String widget, String name, String displayName, boolean mandatory, String defaultValue) {
        FormElement formElement = formElement(name, displayName, mandatory, defaultValue);
        formElement.setWidget(Widget.CUSTOM);
        formElement.setCustomWidget(widget);
        return formElement;
    }

    public static FormElement custom(String widget, String name, String displayName) {
        return custom(widget, name, displayName, true, null);
    }



    public Object doSwitch(WidgetSwitcher switcher, Object...args) {
        return widget.doSwitch(switcher, args);
    }

}
