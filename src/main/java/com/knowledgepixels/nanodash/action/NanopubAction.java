package com.knowledgepixels.nanodash.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nanopub.Nanopub;

import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.Utils;

public abstract class NanopubAction implements Serializable {

	private static final long serialVersionUID = 4086842804225420496L;

	public static final List<NanopubAction> noActions = Collections.emptyList();

	public static final List<NanopubAction> defaultActions;

	public static final List<NanopubAction> ownActions;

	private static Map<String,NanopubAction> defaultClassNameMap = new HashMap<>();

	static {
		List<NanopubAction> da = new ArrayList<>();
		da.add(new CommentAction());
		da.add(new RetractionAction());
		da.add(new ApprovalAction());
		da.add(new UpdateAction());
		da.add(new DeriveAction());
		defaultActions = Collections.unmodifiableList(da);

		List<NanopubAction> oa = new ArrayList<>();
		oa.add(new RetractionAction());
		oa.add(new UpdateAction());
		ownActions = Collections.unmodifiableList(oa);

		for (NanopubAction na : defaultActions) {
			defaultClassNameMap.put(na.getClass().getCanonicalName(), na);
		}
	}

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