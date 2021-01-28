package org.petapico.nanobench.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nanopub.Nanopub;
import org.petapico.nanobench.NanobenchPreferences;
import org.petapico.nanobench.Utils;

public abstract class NanopubAction {

	private static List<NanopubAction> defaultActions = new ArrayList<>();

	static {
		defaultActions.add(new CommentAction());
		defaultActions.add(new RetractionAction());
		defaultActions.add(new ApprovalAction());
		defaultActions = Collections.unmodifiableList(defaultActions);
	}

	public static List<NanopubAction> getDefaultActions() {
		return defaultActions;
	}

	public static List<NanopubAction> getActionsFromPreferences(NanobenchPreferences pref) {
		List<NanopubAction> actions = new ArrayList<>();
		if (pref == null) return actions;
		for (String s : pref.getNanopubActions()) {
			try {
				actions.add((NanopubAction) Class.forName(s).getDeclaredConstructor().newInstance());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return actions;
	}

	public abstract String getLinkLabel(Nanopub np);

	public abstract String getTemplateUri(Nanopub np);

	public abstract String getParamString(Nanopub np);

	public abstract boolean isApplicableToOwnNanopubs();

	public abstract boolean isApplicableToOthersNanopubs();

	public abstract boolean isApplicableTo(Nanopub np);

	protected static String getEncodedUri(Nanopub np) {
		return Utils.urlEncode(np.getUri().stringValue());
	}

}
