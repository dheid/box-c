package edu.unc.lib.boxc.web.common.controllers;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.search.api.exceptions.InvalidHierarchicalFacetException;
import edu.unc.lib.boxc.search.api.requests.HierarchicalBrowseRequest;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.ChildrenCountService;
import edu.unc.lib.boxc.search.solr.services.SearchStateFactory;
import edu.unc.lib.boxc.search.solr.services.SetFacetTitleByIdService;
import edu.unc.lib.boxc.search.solr.utils.SearchStateUtil;
import edu.unc.lib.boxc.web.common.search.SearchActionService;
import edu.unc.lib.boxc.web.common.services.SolrQueryLayerService;
import edu.unc.lib.boxc.web.common.utils.SearchStateSerializationUtil;
import edu.unc.lib.boxc.web.common.utils.SerializationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;

/**
 * Abstract base class for controllers which interact with solr services.
 * @author bbpennel
 */
public abstract class AbstractSolrSearchController {
    private final Logger LOG = LoggerFactory.getLogger(AbstractSolrSearchController.class);

    @Autowired(required = true)
    protected SolrQueryLayerService queryLayer;
    @Autowired(required = true)
    protected SearchActionService searchActionService;
    @Autowired
    protected SearchSettings searchSettings;
    @Autowired
    protected SearchStateFactory searchStateFactory;
    @Autowired
    protected ChildrenCountService childrenCountService;
    @Autowired
    private SetFacetTitleByIdService setFacetTitleByIdService;

    protected SearchRequest generateSearchRequest(HttpServletRequest request) {
        return this.generateSearchRequest(request, null, new SearchRequest());
    }

    protected SearchRequest generateSearchRequest(HttpServletRequest request, SearchState searchState) {
        return this.generateSearchRequest(request, searchState, new SearchRequest());
    }

    /**
     * Builds a search request model object from the provided http servlet request and the provided
     * search state.  If the search state is null, then it will attempt to retrieve it from first
     * the session and if that fails, then from current GET parameters.  Validates the search state
     * and applies any actions provided as well.
     * @param request
     * @return
     */
    protected SearchRequest generateSearchRequest(
            HttpServletRequest request, SearchState searchState, SearchRequest searchRequest) {

        //Get user access groups.  Fill this in later, for now just set to public
        HttpSession session = request.getSession();
        //Get the access group list
        AccessGroupSet accessGroups = getAgentPrincipals().getPrincipals();
        searchRequest.setAccessGroups(accessGroups);

        //Retrieve the last search state
        if (searchState == null) {
            if (searchRequest != null && searchRequest instanceof HierarchicalBrowseRequest) {
                searchState = searchStateFactory.createHierarchicalBrowseSearchState(request.getParameterMap());
            } else {
                searchState = searchStateFactory.createSearchState(request.getParameterMap());
            }
        }

        //Perform actions on search state
        try {
            searchActionService.executeActions(searchState, request.getParameterMap());
        } catch (InvalidHierarchicalFacetException e) {
            LOG.debug("An invalid facet was provided: " + request.getQueryString(), e);
        }

        //Store the search state into the search request
        searchRequest.setSearchState(searchState);

        return searchRequest;
    }

    protected SearchResultResponse getSearchResults(SearchRequest searchRequest) {
        return queryLayer.getSearchResults(searchRequest);
    }

    public SearchActionService getSearchActionService() {
        return searchActionService;
    }

    public void setSearchActionService(SearchActionService searchActionService) {
        this.searchActionService = searchActionService;
    }

    public SolrQueryLayerService getQueryLayer() {
        return queryLayer;
    }

    public void setQueryLayer(SolrQueryLayerService queryLayer) {
        this.queryLayer = queryLayer;
    }

    public void setSearchStateFactory(SearchStateFactory searchStateFactory) {
        this.searchStateFactory = searchStateFactory;
    }

    protected Map<String, Object> getResults(SearchResultResponse resp, String queryMethod,
                                             HttpServletRequest request) {
        AccessGroupSet principals = GroupsThreadStore.getPrincipals();

        childrenCountService.addChildrenCounts(resp.getResultList(),
                principals);

        List<Map<String, Object>> resultList = SerializationUtil.resultsToList(resp, principals);
        Map<String, Object> results = new HashMap<>();
        results.put("metadata", resultList);

        SearchState state = resp.getSearchState();
        // Add display values for filter parameters with separate search and display forms
        setFacetTitleByIdService.populateSearchState(state);

        results.put("pageStart", state.getStartRow());
        results.put("pageRows", state.getRowsPerPage());
        results.put("resultCount", resp.getResultCount());
        results.put("searchStateUrl", SearchStateUtil.generateStateParameterString(state));
        results.put("searchQueryUrl", SearchStateUtil.generateSearchParameterString(state));
        results.put("filterParameters", SearchStateSerializationUtil.getFilterParameters(state));
        results.put("queryMethod", queryMethod);
        if (resp.getFacetFields() != null) {
            results.put("facetFields", resp.getFacetFields());
        }
        results.put("onyen", GroupsThreadStore.getUsername());
        results.put("email", GroupsThreadStore.getEmail());

        if (resp.getSelectedContainer() != null) {
            results.put("container", SerializationUtil.metadataToMap(resp.getSelectedContainer(), principals));
        }

        if (resp.getMinimumDateCreatedYear() != null) {
            results.put("minSearchYear", resp.getMinimumDateCreatedYear());
        }

        return results;
    }
}
