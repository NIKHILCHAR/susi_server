package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.SusiServer;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Constructor;

/**
 * Created by chetankaushik on 30/05/17.
 * This is a servlet that is used to change the role of any user
 * This requires Admin login
 * http://127.0.0.1:4000/aaa/changeRoles this accepts two post parameters-
 * 1. user    --> The username of the user whose role is to be modified
 * 2. role    --> the new role of the user
 */

public class ChangeUserRoles extends AbstractAPIHandler implements APIHandler {
    private static final long serialVersionUID = -1432553481906185711L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() {
        return BaseUserRole.ADMIN;
    }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/aaa/changeRoles";
    }


    @Override
    public JSONObject serviceImpl(Query query, HttpServletResponse response, Authorization authorization, final JsonObjectWithDefault permissions) throws APIException {

        JSONObject result = new JSONObject();
        if (query.get("getServicePermissions", null) != null) {
            String serviceString = query.get("getServicePermissions", null);

            Class<?> serviceClass;
            try {
                serviceClass = Class.forName(serviceString);
            } catch (ClassNotFoundException e) {
                throw new APIException(400, "Bad service name (no class)");
            }
            Constructor<?> constructor;
            try {
                constructor = serviceClass.getConstructors()[0];
            } catch (Throwable e) {
                throw new APIException(400, "Bad service name (no constructor)");
            }
            Object service;
            try {
                service = constructor.newInstance();
            } catch (Throwable e) {
                throw new APIException(400, "Bad service name (no instance possible)");
            }

            if (service instanceof AbstractAPIHandler) {
                result.put("servicePermissions", authorization.getPermissions((AbstractAPIHandler) service));
                return result;
            } else {
                throw new APIException(400, "Bad service name (no instance of AbstractAPIHandler)");
            }
        } else if (query.get("getServiceList", false)) {
            JSONArray serviceList = new JSONArray();
            for (Class<? extends Servlet> service : SusiServer.services) {
                serviceList.put(service.getCanonicalName());
            }
            result.put("serviceList", serviceList);
            return result;
        } else if (query.get("getUserRolePermission", false)) {
            result.put("userRolePermissions", authorization.getUserRole().getPermissionOverrides());
            return result;
        } else {
            // Accept POST params
            String userTobeUpgraded = query.get("user", null);
            String upgradedRole = query.get("role", "user");
            // Validation
            if (userTobeUpgraded == null) {
                throw new APIException(400, "Bad username");
            }
            if (!DAO.authorization.has(userTobeUpgraded)) {
                throw new APIException(400, "Username not found");
            }
            if (!DAO.userRoles.has(upgradedRole))
                throw new APIException(400, "Specified User Role not found");

            // Update the local database
            JSONObject user_obj = new JSONObject();
            user_obj = DAO.authorization.getJSONObject(userTobeUpgraded);
            user_obj.put("userRole", upgradedRole);
            DAO.authorization.put(userTobeUpgraded, user_obj, true);
            DAO.authorization.toJSON();

            // Print Response
            result.put("newDetails", user_obj);
            return result;
        }
    }
}

