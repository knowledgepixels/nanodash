package org.petapico.nanobench.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nanopub.Nanopub;
import org.petapico.nanobench.NanobenchPreferences;
import org.petapico.nanobench.Utils;

public abstract class NanopubAction implements Serializable {

	private static final long serialVersionUID = 4086842804225420496L;

	private static List<NanopubAction> defaultActions = new ArrayList<>();

	private static Map<String,NanopubAction> defaultClassNameMap = new HashMap<>();

	static {
		defaultActions.add(new CommentAction());
		defaultActions.add(new RetractionAction());
		defaultActions.add(new ApprovalAction());
		defaultActions.add(new UpdateAction());
		defaultActions.add(new DeriveAction());
		defaultActions = Collections.unmodifiableList(defaultActions);
		for (NanopubAction na : defaultActions) {
			defaultClassNameMap.put(na.getClass().getCanonicalName(), na);
		}
	}

	public static List<NanopubAction> getDefaultActions() {
		return defaultActions;
	}

	public static List<NanopubAction> getActionsFromPreferences(NanobenchPreferences pref) {
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
