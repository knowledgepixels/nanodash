package com.knowledgepixels.nanodash.action;

import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.Utils;
import org.nanopub.Nanopub;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract class representing an action that can be performed on a Nanopub.
 */
public abstract class NanopubAction implements Serializable {

    private static final long serialVersionUID = 4086842804225420496L;

    public static final NanopubAction[] noActions = new NanopubAction[0];

    /**
     * Default actions that are available for all Nanopubs.
     */
    public static final NanopubAction[] defaultActions;

    public static final NanopubAction[] ownActions;

    private static Map<String, NanopubAction> defaultClassNameMap = new HashMap<>();

    static {
        List<NanopubAction> da = new ArrayList<>();
        da.add(new CommentAction());
        da.add(new RetractionAction());
        da.add(new ApprovalAction());
        da.add(new UpdateAction());
        da.add(new DeriveAction());
        da.add(new UseSameTemplateAction());
        da.add(new UseTemplateAction());
        defaultActions = da.toArray(new NanopubAction[0]);

        List<NanopubAction> oa = new ArrayList<>();
        oa.add(new RetractionAction());
        oa.add(new UpdateAction());
        ownActions = oa.toArray(new NanopubAction[0]);

        for (NanopubAction na : defaultActions) {
            defaultClassNameMap.put(na.getClass().getCanonicalName(), na);
        }
    }

    /**
     * Returns a list of NanopubAction instances based on the preferences.
     *
     * @param pref the NanodashPreferences containing the action class names
     * @return a list of NanopubAction instances
     */
    public static List<NanopubAction> getActionsFromPreferences(NanodashPreferences pref) {
        List<NanopubAction> actions = new ArrayList<>();
        if (pref == null) return actions;
        for (String s : pref.getNanopubActions()) {
            if (defaultClassNameMap.containsKey(s)) continue;
            try {
                NanopubAction na = (NanopubAction) Class.forName(s).getDeclaredConstructor().newInstance();
                actions.add(na);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return actions;
    }

    /**
     * Returns the label for the action link.
     *
     * @param np the Nanopub to which the action applies
     * @return the label for the action link
     */
    public abstract String getLinkLabel(Nanopub np);

    /**
     * Returns the URI of the template associated with the action.
     *
     * @param np the Nanopub to which the action applies
     * @return the URI of the template
     */
    public abstract String getTemplateUri(Nanopub np);

    /**
     * Returns a string representation of the parameters for the action.
     *
     * @param np the Nanopub to which the action applies
     * @return a string representation of the parameters
     */
    public abstract String getParamString(Nanopub np);

    /**
     * Checks if the action is applicable to own Nanopubs.
     *
     * @return true if the action is applicable to own Nanopubs, false otherwise
     */
    public abstract boolean isApplicableToOwnNanopubs();

    /**
     * Checks if the action is applicable to others' Nanopubs.
     *
     * @return true if the action is applicable to others' Nanopubs, false otherwise
     */
    public abstract boolean isApplicableToOthersNanopubs();

    /**
     * Checks if the action is applicable to the given Nanopub.
     *
     * @param np the Nanopub to check
     * @return true if the action is applicable, false otherwise
     */
    public abstract boolean isApplicableTo(Nanopub np);

    protected static String getEncodedUri(Nanopub np) {
        return Utils.urlEncode(np.getUri().stringValue());
    }

}
