package net.floodlightcontroller.routing.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.VirtualGatewayInstance;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4AddressWithMask;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.restlet.data.Status;
import org.restlet.resource.*;

import java.io.IOException;
import java.util.*;

/**
 * @author Qing Wang (qw@g.clemson.edu) at 12/20/17
 */
public class GatewayInstanceResource extends ServerResource {

    private static final String INSTANCE_NOT_FOUND_MESSAGE = "Instance not found.";

    @Get
    public Object getInstance() {
        IRoutingService routingService =
                (IRoutingService) getContext().getAttributes().
                        get(IRoutingService.class.getCanonicalName());

        String whichInstance = (String) getRequestAttributes().get("gateway-name");

        Optional<VirtualGatewayInstance> instance = routingService.getGatewayInstance(whichInstance);

        if (instance.isPresent()) {
            return instance.get();
        }
        else {
            setStatus(Status.CLIENT_ERROR_NOT_FOUND, INSTANCE_NOT_FOUND_MESSAGE);
            return null;
        }

    }


    @Put
    @Post
    public Object updateInstance(String json) {
        IRoutingService routingService =
                (IRoutingService) getContext().getAttributes().
                        get(IRoutingService.class.getCanonicalName());

        String whichInstance = (String) getRequestAttributes().get("gateway-name");

        Optional<VirtualGatewayInstance> instance = routingService.getGatewayInstance(whichInstance);

        if (!instance.isPresent()) {
            setStatus(Status.CLIENT_ERROR_NOT_FOUND, INSTANCE_NOT_FOUND_MESSAGE);
            return null;
        }
        VirtualGatewayInstance gatewayInstance = instance.get();

        if (json == null) {
            setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "One or more required fields missing.");
            return null;
        }

        try {
            JsonNode jsonNode = new ObjectMapper().readTree(json);

            JsonNode gatewayMacNode = jsonNode.get("gateway-mac");
            if (gatewayMacNode != null) {
                gatewayInstance.updateGatewayMac(MacAddress.of(gatewayMacNode.asText()));
            }

            JsonNode switchMembersNode = jsonNode.get("switches");
            if (switchMembersNode != null) {
                for (JsonNode sw : switchMembersNode) {
                    JsonNode dpidNode = sw.get("dpid");
                    if (dpidNode != null) {
                        gatewayInstance.addSwitchMember(DatapathId.of(dpidNode.asText()));
                    }
                }
            }

            JsonNode switchportsNode = jsonNode.get("switchports");
            if (switchportsNode != null) {
                for (JsonNode swpt : switchportsNode) {
                    JsonNode dpidNode = swpt.get("dpid");
                    JsonNode portNode = swpt.get("port");
                    if (dpidNode != null && portNode != null) {
                        NodePortTuple npt = new NodePortTuple(DatapathId.of(dpidNode.asText()), OFPort.of(portNode.asInt()));
                        gatewayInstance.addNptMember(npt);
                    }
                }
            }

            JsonNode subnetsNode = jsonNode.get("subnets");
            if (subnetsNode != null) {
                for (JsonNode subnet : subnetsNode) {
                    JsonNode subnetNode = subnet.get("subnet");
                    if (subnetNode != null) {
                        gatewayInstance.addSubnetMember(IPv4AddressWithMask.of(subnetNode.asText()));
                    }
                }
            }

            setDescription("Instance updated.");
            return gatewayInstance;

        }
        catch (IOException e) {
            setStatus(org.restlet.data.Status.CLIENT_ERROR_BAD_REQUEST, "Instance object could not be deserialized.");
            return e;
        }

    }


    @Delete
    public Object deleteInstance() {
        IRoutingService routingService =
                (IRoutingService) getContext().getAttributes().
                        get(IRoutingService.class.getCanonicalName());

        String whichInstance = (String) getRequestAttributes().get("gateway-name");

        if (routingService.deleteGatewayInstance(whichInstance)) {
            return ImmutableMap.of("deleted", whichInstance);
        }
        else {
            setStatus(Status.CLIENT_ERROR_NOT_FOUND, INSTANCE_NOT_FOUND_MESSAGE);
            return null;
        }

    }

}
