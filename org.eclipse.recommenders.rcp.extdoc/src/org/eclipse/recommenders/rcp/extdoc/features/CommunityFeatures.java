package org.eclipse.recommenders.rcp.extdoc.features;

import org.eclipse.recommenders.commons.utils.names.IName;
import org.eclipse.recommenders.rcp.extdoc.IProvider;
import org.eclipse.swt.widgets.Composite;

import com.google.common.base.Preconditions;

public final class CommunityFeatures {

    private IProvider provider;
    private IUserFeedbackServer server;
    private IName element;
    private IUserFeedback feedback;

    private CommentsComposite comments;
    private StarsRatingComposite ratings;

    public static CommunityFeatures create(final IName element, final IProvider provider,
            final IUserFeedbackServer server) {
        final CommunityFeatures features = new CommunityFeatures();
        features.provider = provider;
        features.server = Preconditions.checkNotNull(server);
        features.element = element;
        features.feedback = server.getUserFeedback(element, provider);
        return features;
    }

    public CommentsComposite loadCommentsComposite(final Composite parent) {
        comments = CommentsComposite.create(element, provider, feedback, server, parent);
        return comments;
    }

    public StarsRatingComposite loadStarsRatingComposite(final Composite parent) {
        ratings = new StarsRatingComposite(element, provider, feedback, server, parent);
        return ratings;
    }

    public void dispose() {
        if (comments != null) {
            comments.dispose();
        }
        if (ratings != null) {
            ratings.dispose();
        }
    }

}
