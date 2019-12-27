package com.soonsoft.uranus.services.membership;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.soonsoft.uranus.core.Guard;
import com.soonsoft.uranus.core.common.event.IEventListener;
import com.soonsoft.uranus.core.common.event.SimpleEventListener;
import com.soonsoft.uranus.core.common.collection.MapUtils;

import com.soonsoft.uranus.util.caching.Cache;

import com.soonsoft.uranus.security.authorization.IFunctionManager;
import com.soonsoft.uranus.security.entity.MenuInfo;
import com.soonsoft.uranus.security.entity.RoleInfo;
import com.soonsoft.uranus.security.entity.UserInfo;
import com.soonsoft.uranus.services.membership.dao.AuthRolesInFunctionsDAO;
import com.soonsoft.uranus.services.membership.dao.SysFunctionDAO;
import com.soonsoft.uranus.services.membership.dao.SysMenuDAO;
import com.soonsoft.uranus.services.membership.dto.AuthRole;
import com.soonsoft.uranus.services.membership.dto.AuthRoleIdAndFunctionId;
import com.soonsoft.uranus.services.membership.dto.SysMenu;
import com.soonsoft.uranus.services.membership.model.Transformer;

import org.springframework.util.CollectionUtils;
import org.springframework.security.core.GrantedAuthority;

/**
 * FunctionService
 */
public class FunctionService implements IFunctionManager {

    private SysFunctionDAO functionDAO;

    private SysMenuDAO menuDAO;

    private AuthRolesInFunctionsDAO rolesInFunctionsDAO;

    private Cache<String, MenuInfo> menuStore;

    private List<String> sequence;

    private static final Object locker = new Object();

    /** 事件定义 */
    public IEventListener<SysMenu> changed = new SimpleEventListener<>("MenuEvent");

    public FunctionService() {
        this(null);
    }

    public FunctionService(Cache<String, MenuInfo> menuStore) {
        if(menuStore != null) {
            this.menuStore = menuStore;
        } else {
            this.menuStore = new Cache<>(128);
        }

        // 注册缓存更新
        changed.on(m -> {
            MenuInfo menuInfo = Transformer.toMenuInfo(m);
            MenuInfo oldMenuInfo = menuStore.get(menuInfo.getResourceCode());
            menuInfo.setAllowRoles(oldMenuInfo.getAllowRoles());
            menuStore.put(menuInfo.getResourceCode(), menuInfo);
        });
    }

    public SysFunctionDAO getFunctionDAO() {
        return functionDAO;
    }

    public void setFunctionDAO(SysFunctionDAO functionDAO) {
        this.functionDAO = functionDAO;
    }

    public SysMenuDAO getMenuDAO() {
        return menuDAO;
    }

    public void setMenuDAO(SysMenuDAO menuDAO) {
        this.menuDAO = menuDAO;
    }

    public AuthRolesInFunctionsDAO getRolesInFunctionsDAO() {
        return rolesInFunctionsDAO;
    }

    public void setRolesInFunctionsDAO(AuthRolesInFunctionsDAO rolesInFunctionsDAO) {
        this.rolesInFunctionsDAO = rolesInFunctionsDAO;
    }

    public void setMenuStore(Cache<String, MenuInfo> menuStore) {
        this.menuStore = menuStore;
    }

    //#region IFunctionManager methods

    @Override
    public List<MenuInfo> getEnabledMenus() {
        if(menuStore != null) {
            List<String> mySequence = this.sequence;
            if(mySequence != null) {
                List<MenuInfo> records = new ArrayList<>(mySequence.size());
                for(String code : mySequence) {
                    MenuInfo menu = menuStore.get(code);
                    if(menu != null) {
                        records.add(menu);
                    }
                }
                // 检查是否全部命中
                if(records.size() == mySequence.size()) {
                    return records;
                }
            }
        }

        Map<String, Object> params = MapUtils.createHashMap(1);
        params.put("status", SysMenu.STATUS_ENABLED);
        List<SysMenu> menus = getAllMenus(params);
        List<MenuInfo> records = new ArrayList<>(menus.size());

        if(menuStore != null && menus != null && !menus.isEmpty()) {
            Map<String, MenuInfo> cacheValue = MapUtils.createHashMap(menus.size());
            List<String> sequence = new ArrayList<>(menus.size());
            menus.forEach(i -> {
                MenuInfo menuInfo = Transformer.toMenuInfo(i);
                sequence.add(menuInfo.getResourceCode());
                records.add(menuInfo);
                cacheValue.put(menuInfo.getResourceCode(), menuInfo);
            });
            menuStore.putAll(cacheValue);
            synchronized(locker) {
                this.sequence = sequence;
            }
        }

        return records;
    }

    @Override
    public List<MenuInfo> getMenus(UserInfo user) {
        if(user != null) {
            Collection<GrantedAuthority> authorities =  user.getAuthorities();
            if(authorities != null && !authorities.isEmpty()) {
                Set<String> userRoles = new HashSet<>();
                authorities.forEach(i -> userRoles.add(i.getAuthority()));
                
                List<MenuInfo> menus = getEnabledMenus();
                List<MenuInfo> userMenus = new ArrayList<>(menus.size());
                for(MenuInfo menu : menus) {
                    List<RoleInfo> roles = menu.getAllowRoles();
                    for(RoleInfo role : roles) {
                        if(userRoles.contains(role.getAuthority())) {
                            userMenus.add(menu);
                            break;
                        }
                    }
                }
                return userMenus;
            }
        }
        return null;
    }

    //#endregion

    public List<SysMenu> getAllMenus(Map<String, Object> params) {
        List<SysMenu> menus = menuDAO.select(params);

        if(menus == null) {
            return new ArrayList<>(0);
        }
        List<String> functionIdList = new ArrayList<>(menus.size());
        menus.forEach(i -> {
            functionIdList.add(i.getFunctionId());
        });

        Map<String, Set<Object>> functionRoleMap = rolesInFunctionsDAO.selectByFunctions(functionIdList, 1);
        if(functionRoleMap != null) {
            menus.forEach(i -> {
                Set<Object> roleSet = functionRoleMap.get(i.getFunctionId());
                if(roleSet != null) {
                    for(Object item : roleSet) {
                        i.addRole((AuthRole) item);
                    }
                }
            });
        }
        return menus;
    }

    public boolean updateMenu(SysMenu menu) {
        Guard.notNull(menu, "the SysMenu is required.");
        Guard.notEmpty(menu.getFunctionId(), "the SysMenu.functionId is required.");
        
        int effectRows = 0;

        Collection<AuthRole> roles = menu.getRoles();
        if(!CollectionUtils.isEmpty(roles)) {
            rolesInFunctionsDAO.deleteByFunctionId(menu.getFunctionId());
            for(AuthRole role : roles) {
                AuthRoleIdAndFunctionId roleIdFunctionId = new AuthRoleIdAndFunctionId();
                roleIdFunctionId.setRoleId(role.getRoleId());
                roleIdFunctionId.setFunctionId(menu.getFunctionId());
                effectRows += rolesInFunctionsDAO.insert(roleIdFunctionId);
            }
        }
        
        effectRows += functionDAO.update(menu);

        boolean result = effectRows > 0;
        if(result) {
            changed.trigger(menu);
        }
        return result;
    }

}