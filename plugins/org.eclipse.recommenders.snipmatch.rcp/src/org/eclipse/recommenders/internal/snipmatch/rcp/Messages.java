package org.eclipse.recommenders.internal.snipmatch.rcp;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.recommenders.internal.snipmatch.rcp.messages"; //$NON-NLS-1$
    public static String COMPLETION_ENGINE_NO_SNIPPETS_FOUND;

    public static String DIALOG_MESSAGE_ENTER_NEW_KEYWORD;
    public static String DIALOG_MESSAGE_NEW_SNIPPET_REPOSITORY;
    public static String DIALOG_MESSAGE_NO_REPOSITORY_AVAILABLE;
    public static String DIALOG_MESSAGE_SAVE_SNIPPET_WITH_MODIFIED_CODE;
    public static String DIALOG_OPTION_CANCEL;
    public static String DIALOG_OPTION_OVERWRITE;
    public static String DIALOG_OPTION_STORE_AS_NEW;
    public static String DIALOG_TITLE_ENTER_NEW_KEYWORD;
    public static String DIALOG_TITLE_ERROR_WHILE_STORING_SNIPPET;
    public static String DIALOG_TITLE_NEW_SNIPPET_REPOSITORY;
    public static String DIALOG_TITLE_SAVE_SNIPPET;
    public static String DIALOG_VALIDATOR_KEYWORD_ALREADY_ADDED;

    public static String EDITOR_BUTTON_ADD_KEYWORDS;
    public static String EDITOR_BUTTON_REMOVE_KEYWORDS;
    public static String EDITOR_LABEL_SNIPPET_DESCRIPTION;
    public static String EDITOR_LABEL_SNIPPET_NAME;
    public static String EDITOR_LABEL_SNIPPET_UUID;
    public static String EDITOR_LABEL_SNIPPETS_KEYWORD;
    public static String EDITOR_LABEL_TITLE_METADATA;
    public static String EDITOR_PAGE_NAME_METADATA;
    public static String EDITOR_PAGE_NAME_SOURCE;

    public static String JOB_OPENING_SNIPPET_REPOSITORY;
    public static String JOB_REFRESHING_SNIPPETS_VIEW;

    public static String PREFPAGE_LABEL_REMOTE_SNIPPETS_REPOSITORY;
    public static String PREFPAGE_PREFPAGE_DESCRIPTION;

    public static String SEARCH_PLACEHOLDER_FILTER_TEXT;

    public static String SNIPPETS_VIEW_BUTTON_ADD;
    public static String SNIPPETS_VIEW_BUTTON_EDIT;
    public static String SNIPPETS_VIEW_BUTTON_REFRESH;
    public static String SNIPPETS_VIEW_BUTTON_REMOVE;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
