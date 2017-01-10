package com.yeetclub.android.enums;

/**
 * @author shuklaalok7
 * @since 9/12/2016
 */
public enum NotificationType {
    LIKE("pushLike"), REPLY("pushReply");

    private String pushFunction;

    NotificationType(String pushFunction) {
        this.pushFunction = pushFunction;
    }

    /**
     * @param searchTerm
     * @return
     */
    public static NotificationType search(final String searchTerm) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            return null;
        }

        String curatedSearchTerm = searchTerm.toUpperCase().replace("TYPE", "");

        for (NotificationType group : NotificationType.values()) {
            if (curatedSearchTerm.equalsIgnoreCase(group.name())) {
                return group;
            }
        }
        return null;
    }

    /**
     * @return The key to be used for grouping the notifications
     */
    public String getKey() {
        return "com.yitter." + this.name();
    }

    public String getPushFunction() {
        return pushFunction;
    }

}
