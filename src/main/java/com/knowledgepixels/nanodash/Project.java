package com.knowledgepixels.nanodash;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

/**
 * Class representing a Nanodash project.
 */
public class Project implements Serializable {

    private static List<Project> projectList = new ArrayList<>();
    private static ConcurrentMap<String,Project> projectsByCoreInfo = new ConcurrentHashMap<>();
    private static ConcurrentMap<String,Project> projectsById = new ConcurrentHashMap<>();

    public static synchronized void refresh(ApiResponse resp) {
        projectList.clear();
        ConcurrentMap<String,Project> prevProjectsByCoreInfoPrev = projectsByCoreInfo;
        projectsByCoreInfo = new ConcurrentHashMap<>();
        projectsById.clear();
        for (ApiResponseEntry entry : resp.getData()) {
            Project project = new Project(entry.get("project"), entry.get("label"), entry.get("np"));
            Project prevProject = prevProjectsByCoreInfoPrev.get(project.getCoreInfoString());
            if (prevProject != null) project = prevProject;
            projectList.add(project);
            projectsByCoreInfo.put(project.getCoreInfoString(), project);
            projectsById.put(project.getId(), project);
        }
    }

    public static List<Project> getProjectList() {
        return projectList;
    }

    public static Project get(String id) {
        return projectsById.get(id);
    }

    private String id, label, rootNanopubId;
    private Nanopub rootNanopub = null;

    private Project(String id, String label, String rootNanopubId) {
        this.id = id;
        this.label = label;
        this.rootNanopubId = rootNanopubId;
    }

    public String getId() {
        return id;
    }

    public String getRootNanopubId() {
        return rootNanopubId;
    }

    public String getCoreInfoString() {
        return id + " " + rootNanopubId;
    }

    public Nanopub getRootNanopub() {
        if (rootNanopub == null) {
            rootNanopub = Utils.getAsNanopub(rootNanopubId);
        }
        return rootNanopub;
    }

    public String getLabel() {
        return label;
    }
}
