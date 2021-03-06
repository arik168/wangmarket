package com.xnx3.j2ee.controller.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.xnx3.j2ee.Global;
import com.xnx3.j2ee.entity.User;
import com.xnx3.j2ee.service.SqlService;
import com.xnx3.j2ee.service.UserService;
import com.xnx3.j2ee.shiro.ActiveUser;
import com.xnx3.j2ee.controller.BaseController;
import com.xnx3.j2ee.util.Page;
import com.xnx3.j2ee.util.Sql;
import com.xnx3.j2ee.vo.BaseVO;

/**
 * 用户管理
 * @author 管雷鸣
 */
@Controller
@RequestMapping("/admin/user")
public class UserAdminController_ extends BaseController {
	@Resource
	private UserService userService;
	@Resource
	private SqlService sqlService;
	
	/**
	 * 删除用户
	 * @param id 要删除的用户id，User.id
	 */
	@RequiresPermissions("adminUserDelete")
	@RequestMapping(value="deleteUser${url.suffix}", method = RequestMethod.POST)
	@ResponseBody
	public BaseVO deleteUser(@RequestParam(value = "id", required = true) int id){
		if(id>0){
			User u = sqlService.findById(User.class, id);
			if(u!=null){
				sqlService.delete(u);
				return success();
			}
		}
		
		return error("删除失败");
	}
	
	/**
	 * 用户列表
	 */
	@RequiresPermissions("adminUserList")
	@RequestMapping("list${url.suffix}")
	public String list(HttpServletRequest request,Model model){
		Sql sql = new Sql(request);
		sql.setSearchColumn(new String[]{"username","email","nickname","phone","id=","regtime(date:yyyy-MM-dd hh:mm:ss)>"});
		int count = sqlService.count("user", sql.getWhere());
		Page page = new Page(count, Global.getInt("LIST_EVERYPAGE_NUMBER"), request);
		sql.setSelectFromAndPage("SELECT * FROM user", page);
		sql.setDefaultOrderBy("user.id DESC");
		sql.setOrderByField(new String[]{"id","lasttime","money","currency"});
		List<User> list = sqlService.findBySql(sql, User.class);
		
		model.addAttribute("page", page);
		model.addAttribute("list", list);
		return "/iw/admin/user/list";
	}
	
	/**
	 * 用户详情
	 * @param id 要查看详情的用户的id, 对应 user.id
	 */
	@RequiresPermissions("adminUserView")
	@RequestMapping("view${url.suffix}")
	public String view(@RequestParam(value = "id", required = true) int id,Model model){
		User user = sqlService.findById(User.class, id);
		if(user == null){
			return error(model, "要查看的用户不存在");
		}
		
		if(user.getReferrerid()==null || user.getReferrerid()==0){
			model.addAttribute("referrer", "无邀请人");
		}else{
			User parentUser = sqlService.findById(User.class, user.getReferrerid());
			model.addAttribute("referrer", "<a href='view.do?id="+user.getReferrerid()+"'>id:"+user.getReferrerid()+","+parentUser.getUsername()+"</a>");
		}
		
		model.addAttribute("u", user);
		return "/iw/admin/user/view";
	}
	
	/**
	 * 冻结／解除冻结用户
	 * @param id {@link User}.id
	 * @param isfreeze 要更改的值
	 */
	@RequiresPermissions("adminUserUpdateFreeze")
	@RequestMapping("updateFreeze${url.suffix}")
	public String updateFreeze(@RequestParam(value = "id", required = true) int id,
			@RequestParam(value = "isfreeze", required = true) int isfreeze,
			Model model){
		BaseVO baseVO = new BaseVO();
		if(isfreeze==User.ISFREEZE_FREEZE){
			baseVO = userService.freezeUser(id);
		}else if (isfreeze==User.ISFREEZE_NORMAL) {
			baseVO = userService.unfreezeUser(id);
		}else{
			baseVO.setBaseVO(BaseVO.FAILURE, "未知参数！");
		}
		
		if(baseVO.getResult() == BaseVO.SUCCESS){
			return success(model, "操作成功！","admin/user/view.do?id="+id);
		}else{
			return error(model, "操作失败！");
		}
	}
	
}
