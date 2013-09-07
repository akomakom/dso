/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package net.sf.ehcache.management.resource.services.validator.impl;

import com.terracotta.management.service.TsaManagementClientService;
import net.sf.ehcache.management.resource.services.validator.AbstractEhcacheRequestValidator;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.AgentEntity;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;

import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p/>
 * {@inheritDoc}
 *
 * @author Ludovic Orban
 */
public final class JmxEhcacheRequestValidator extends AbstractEhcacheRequestValidator {

  private static final int MAXIMUM_CLIENTS_TO_DISPLAY = Integer.getInteger("com.terracotta.agent.defaultMaxClientsToDisplay", 64);
  private final TsaManagementClientService tsaManagementClientService;

  private static final ThreadLocal<Set<String>> tlNode = new ThreadLocal<Set<String>>();

  public JmxEhcacheRequestValidator(TsaManagementClientService tsaManagementClientService) {
    this.tsaManagementClientService = tsaManagementClientService;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void validateSafe(UriInfo info) {
    validateAgentSegment(info.getPathSegments());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void validateAgentSegment(List<PathSegment> pathSegments) {
    String ids = pathSegments.get(0).getMatrixParameters().getFirst("ids");

    try {
      Set<String> nodes = tsaManagementClientService.getRemoteAgentNodeNames();
      if (ids == null) {
        // the user did not specify which clients to display; let's return them all if there aren't too many
        if (nodes.size() <= MAXIMUM_CLIENTS_TO_DISPLAY) {
          setValidatedNodes(nodes);
        } else {
          throw new ResourceRuntimeException(
                  String.format("There are more than %s agents available; you have to change the maximum using the" +
                          " VM argument com.terracotta.agent.defaultMaxClientsToDisplay or you have to specify each" +
                          " agent ID. Agent IDs must be in '%s' or '%s'.", MAXIMUM_CLIENTS_TO_DISPLAY, nodes, AgentEntity.EMBEDDED_AGENT_ID),
                  Response.Status.BAD_REQUEST.getStatusCode());
        }
      } else {
        String[] idsArray = ids.split(",");

        for (String id : idsArray) {
          if (!nodes.contains(id) && !AgentEntity.EMBEDDED_AGENT_ID.equals(id)) {
            throw new ResourceRuntimeException(
                    String.format("Agent IDs must be in '%s' or '%s'.", nodes, AgentEntity.EMBEDDED_AGENT_ID),
                    Response.Status.BAD_REQUEST.getStatusCode());
          }
        }

        setValidatedNodes(new HashSet<String>(Arrays.asList(idsArray)));
      }
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException(
              "Unexpected error validating request.",
              see,
              Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  public Set<String> getValidatedNodes() {
    return tlNode.get();
  }

  public String getSingleValidatedNode() {
    if (tlNode.get().size() != 1) {
      throw new RuntimeException("A single node ID must be specified, got: " + tlNode.get());
    }
    return tlNode.get().iterator().next();
  }

  public void setValidatedNodes(Set<String> node) {
    tlNode.set(node);
  }

}
