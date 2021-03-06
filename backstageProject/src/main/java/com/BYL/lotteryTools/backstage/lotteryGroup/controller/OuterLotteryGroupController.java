package com.BYL.lotteryTools.backstage.lotteryGroup.controller;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.BYL.lotteryTools.backstage.lotteryGroup.dto.LotteryGroupDTO;
import com.BYL.lotteryTools.backstage.lotteryGroup.dto.RelaApplyOfLbuyerorexpertAndGroupDTO;
import com.BYL.lotteryTools.backstage.lotteryGroup.entity.LGroupLevel;
import com.BYL.lotteryTools.backstage.lotteryGroup.entity.LotteryGroup;
import com.BYL.lotteryTools.backstage.lotteryGroup.entity.RelaApplyOfLbuyerorexpertAndGroup;
import com.BYL.lotteryTools.backstage.lotteryGroup.entity.RelaBindOfLbuyerorexpertAndGroup;
import com.BYL.lotteryTools.backstage.lotteryGroup.entity.RelaGroupUpLevelRecord;
import com.BYL.lotteryTools.backstage.lotteryGroup.service.LGroupLevelService;
import com.BYL.lotteryTools.backstage.lotteryGroup.service.LotteryGroupService;
import com.BYL.lotteryTools.backstage.lotteryGroup.service.RelaApplybuyerAndGroupService;
import com.BYL.lotteryTools.backstage.lotteryGroup.service.RelaBindbuyerAndGroupService;
import com.BYL.lotteryTools.backstage.lotteryGroup.service.RelaGroupUpLevelService;
import com.BYL.lotteryTools.backstage.lotterybuyerOfexpert.dto.LotterybuyerOrExpertDTO;
import com.BYL.lotteryTools.backstage.lotterybuyerOfexpert.entity.LotterybuyerOrExpert;
import com.BYL.lotteryTools.backstage.lotterybuyerOfexpert.service.LotterybuyerOrExpertService;
import com.BYL.lotteryTools.backstage.outer.controller.PushController;
import com.BYL.lotteryTools.backstage.outer.repository.rongYunCloud.io.rong.models.CodeSuccessResult;
import com.BYL.lotteryTools.backstage.outer.service.RongyunImService;
import com.BYL.lotteryTools.backstage.user.entity.City;
import com.BYL.lotteryTools.backstage.user.entity.Province;
import com.BYL.lotteryTools.backstage.user.service.CityService;
import com.BYL.lotteryTools.backstage.user.service.ProvinceService;
import com.BYL.lotteryTools.common.bean.ResultBean;
import com.BYL.lotteryTools.common.entity.Uploadfile;
import com.BYL.lotteryTools.common.service.UploadfileService;
import com.BYL.lotteryTools.common.util.BeanUtil;
import com.BYL.lotteryTools.common.util.Constants;
import com.BYL.lotteryTools.common.util.QRCodeUtil;
import com.BYL.lotteryTools.common.util.QueryResult;

/**
 *彩聊群外部接口类
* @Description: TODO(这里用一句话描述这个方法的作用) 
* @author banna
* @date 2017年5月11日 下午3:24:54
 */
@Controller
@RequestMapping("/outerLGroup")
public class OuterLotteryGroupController
{
	private Logger logger = LoggerFactory.getLogger(OuterLotteryGroupController.class);
	
	@Autowired
	private RongyunImService rongyunImService;
	
	@Autowired
	private LotteryGroupService lotteryGroupService;
	
	@Autowired
	private LotterybuyerOrExpertService lotterybuyerOrExpertService;
	
	@Autowired
	private UploadfileService uploadfileService;
	
	@Autowired
	private RelaBindbuyerAndGroupService relaBindbuyerAndGroupService;
	
	@Autowired
	private LGroupLevelService lGroupLevelService;
	
	@Autowired
	private RelaGroupUpLevelService relaGroupUpLevelService;
	
	@Autowired
	private RelaApplybuyerAndGroupService relaApplybuyerAndGroupService;
	
	@Autowired
	private ProvinceService provinceService;
	
	@Autowired
	private CityService cityService;
	
	public static final String SUCCESS_CODE = "200";//成功返回码
	
	/**
	 * 删除群
	* @Title: deleteGroup 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param dto
	* @param @param request
	* @param @param httpSession
	* @param @return    设定文件 
	* @author banna
	* @date 2017年4月24日 下午1:51:25 
	* @return Map<String,Object>    返回类型 
	* @throws
	 */
	@RequestMapping(value="/deleteGroup", method = RequestMethod.GET)
	public @ResponseBody Map<String,Object> deleteGroup(
			LotteryGroupDTO dto,
			HttpServletRequest request,HttpSession httpSession)
	{
		Map<String,Object> map = new HashMap<String, Object>();
		
		//删除群的同时删除群绑定的机器人
		LotteryGroup entity = lotteryGroupService.getLotteryGroupById(dto.getId());
		
		if(null != entity)
		{
			String groupId = entity.getId();
			entity.setGroupRobotID(null);
			//删除融云的群信息
			CodeSuccessResult result = rongyunImService.groupDismiss(entity.getLotteryBuyerOrExpert().getId(), groupId);
			if(!OuterLotteryGroupController.SUCCESS_CODE.equals(result.getCode().toString()))
			{
				logger.error("融云删除群报错", result.getErrorMessage());
			}
			
			//删除数据库中的群信息
			entity.setIsDeleted(Constants.IS_DELETED);
			entity.setModify(dto.getOwnerId());
			entity.setModifyTime(new Timestamp(System.currentTimeMillis()));
			entity.setLotteryBuyerOrExpert(null);
			
			
			//删除群的同时删除群的关联关系(用户和群的关联关系)
			Pageable pageable = new PageRequest(0,Integer.MAX_VALUE);
			QueryResult<RelaBindOfLbuyerorexpertAndGroup> relas = relaBindbuyerAndGroupService.getMemberOfJoinGroup(pageable, groupId);
			List<RelaBindOfLbuyerorexpertAndGroup> list = relas.getResultList();
			for (RelaBindOfLbuyerorexpertAndGroup relaBindOfLbuyerorexpertAndGroup : list) 
			{
				try
				{
					relaBindbuyerAndGroupService.delete(relaBindOfLbuyerorexpertAndGroup);
				}
				catch(Exception e)
				{
					logger.error("delete,error:", e);
				}
			}
			//删除加群申请的关联关系
			List<RelaApplyOfLbuyerorexpertAndGroup> applys = relaApplybuyerAndGroupService.
					getRelaApplyOfLbuyerorexpertAndGroupByGroupId(groupId);
			for (RelaApplyOfLbuyerorexpertAndGroup delApply : applys) 
			{
				relaApplybuyerAndGroupService.delete(delApply);
			}
			
			//删除群等级关联关系
			List<RelaGroupUpLevelRecord> records = relaGroupUpLevelService.getRelaGroupUpLevelRecordByGroupId(groupId);
			for (RelaGroupUpLevelRecord relaGroupUpLevelRecord : records) {
				relaGroupUpLevelService.delete(relaGroupUpLevelRecord);
			}
			
			//删除群头像和群二维码
			List<Uploadfile> touxiang = uploadfileService.getUploadfilesByNewsUuid(entity.getTouXiang());
			uploadfileService.deleteUploadFile(touxiang, httpSession);//调用删除附件数据和附件文件方法
			//删除二维码图片
			String savePath = httpSession.getServletContext().getRealPath(entity.getGroupQRImg());//获取二维码绝对路径
			File dirFile = new File(savePath);
			boolean deleteFlag = dirFile.delete();
			if(deleteFlag)
				logger.info("删除成功",deleteFlag);
			else
				logger.error("error:","删除失败，文件："+entity.getGroupQRImg());//若删除失败记录未删除成功的文件,之后做手动删除
			
			
			lotteryGroupService.update(entity);
			map.put("message", "删除成功");
			map.put("flag", true);
		}
		else
		{
			map.put("message", "删除失败");
			map.put("flag", false);
		}
		
		return map;
	}
	
	/**
	 * 获取当前群的群成员
	* @Title: getMembersOfGroup 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param groupId
	* @param @param request
	* @param @param httpSession
	* @param @return    设定文件 
	* @author banna
	* @date 2017年4月24日 下午4:44:23 
	* @return Map<String,Object>    返回类型 
	* @throws
	 */
	@RequestMapping(value="/getMembersOfGroup", method = RequestMethod.GET)
	public @ResponseBody Map<String,Object> getMembersOfGroup(
			@RequestParam(value="page",required=false)   Integer page,//当前页数
			@RequestParam(value="rows",required=false)    Integer rows,//当前获取数据量
			String groupId,
			HttpServletRequest request,HttpSession httpSession)
	{
		Map<String,Object> map = new HashMap<String, Object>();
		
		LotteryGroup group = lotteryGroupService.getLotteryGroupById(groupId);
		//获取当前群和用户的关联关系(TODO:当前方法获取的群成员不包括群主和群内机器人)
		Pageable pageable = null;
		if(null != rows && 0 != rows)
		{
			pageable = new PageRequest(page-1,rows);
		}
		else
		{
			pageable = new PageRequest(0,Integer.MAX_VALUE);
		}
		
		//不带分页的群成员查询
		QueryResult<RelaBindOfLbuyerorexpertAndGroup> lQueryResult = relaBindbuyerAndGroupService.getMemberOfJoinGroup(pageable, groupId);
		List<RelaBindOfLbuyerorexpertAndGroup> relalist = lQueryResult.getResultList();
		
		List<LotterybuyerOrExpertDTO> userDtos = new ArrayList<LotterybuyerOrExpertDTO>();
		LotterybuyerOrExpert owner = group.getLotteryBuyerOrExpert();
		try
		{
			for (RelaBindOfLbuyerorexpertAndGroup rela : relalist)
			{
				LotterybuyerOrExpertDTO dto = new  LotterybuyerOrExpertDTO();
				dto = lotterybuyerOrExpertService.toDTO(rela.getLotterybuyerOrExpert());
				dto.setIsGroupOwner(rela.getIsGroupOwner());
				userDtos.add(dto);
					
			}
			
			 map.put("flag", true);
			 map.put("message", "获取成功");
			 map.put("memberDtos", userDtos);
			 map.put("rows",userDtos);
			 map.put("total", lQueryResult.getTotalCount());
		}
		catch(Exception e)
		{
			logger.error("error:", e);
			 map.put("flag", false);
			 map.put("message", "获取失败");
		}
		
		
		return map;
	}
	
	/**
	 * 获取群列表
	* @Title: getGroupList 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param dto
	* @param @param request
	* @param @param httpSession
	* @param @return    设定文件 
	* @author banna
	* @date 2017年4月24日 下午5:06:49 
	* @return Map<String,Object>    返回类型 
	* @throws
	 */
	@RequestMapping(value="/getGroupList", method = RequestMethod.GET)
	public @ResponseBody Map<String,Object> getGroupList(
			LotteryGroupDTO dto,
			@RequestParam(value="userId",required=false)   String userId,//当前发出获取群列表的的用户id
			@RequestParam(value="page",required=false)   Integer page,//当前页数
			@RequestParam(value="row",required=false)    Integer row,//当前获取数据量
			HttpServletRequest request,HttpSession httpSession)
	{
		Map<String,Object> map = new HashMap<String, Object>();
		
		//放置分页参数
		Pageable pageable = new PageRequest(0,Integer.MAX_VALUE);
		
		//参数
		StringBuffer buffer = new StringBuffer();
		List<Object> params = new ArrayList<Object>();
		boolean flag = false;
		
		//只查询未删除数据
		params.add("1");//只查询有效的数据
		buffer.append(" isDeleted = ?").append(params.size());
		
		//传汉字
		List<Province> prolist = provinceService.getProvinceByPname("%"+dto.getProvince()+"%");
		if(null != prolist && prolist.size()>0)
		{
			flag = true;
			params.add(prolist.get(0).getPcode());
			buffer.append(" and province = ?").append(params.size());
		}
		else
		{
			//传汉字
			List<City> cityList = cityService.getCityByCname("%"+dto.getCity()+"%");
			if(null != cityList && cityList.size()>0)
			{
				flag = true;
				params.add(cityList.get(0).getCcode());
				buffer.append(" and city = ?").append(params.size());
			}
			else
			{
				//传汉字
				String lotteryType = "";
				if(null != dto.getLotteryType() &&!"".equals(dto.getLotteryType()))
				{
					lotteryType = "体彩".equals(dto.getLotteryType())?"1":"2";
				}
				if(!"".equals(lotteryType))
				{
					flag = true;
					params.add(lotteryType);
					buffer.append(" and lotteryType = ?").append(params.size());
				}
				else
				{
					//按群号精确查找
					if(null != dto.getGroupNumber() && !"".equals(dto.getGroupNumber()))
					{
						flag = true;
						params.add(dto.getGroupNumber());
						buffer.append(" and groupNumber = ?").append(params.size());
					}
					else
					{
						//按群名称精确查找
						if(null != dto.getName() && !"".equals(dto.getName()))
						{
							flag = true;
							params.add(dto.getName());
							buffer.append(" and name = ?").append(params.size());
						}
					}
				}
				
			}
			
			
		}		
		
		
		//排序
		List<LotteryGroupDTO> dtos =  null;
		if(flag)
		{
			LinkedHashMap<String, String> orderBy = new LinkedHashMap<String, String>();
			orderBy.put("createTime", "desc");
			
			QueryResult<LotteryGroup> lQueryResult = lotteryGroupService
					.getLotteryGroupList(LotteryGroup.class,
					buffer.toString(), params.toArray(),orderBy, pageable);
					
			List<LotteryGroup> list = lQueryResult.getResultList();
			
			dtos = lotteryGroupService.toDTOs(list);
			
			if(null != userId && !"".equals(userId))
			{
				for (LotteryGroupDTO group : dtos) 
				{
					//判断当前用户是否已加入此群
					RelaBindOfLbuyerorexpertAndGroup rela = relaBindbuyerAndGroupService.
							getRelaBindOfLbuyerorexpertAndGroupByUserIdAndGroupId(userId, group.getId());
					
					if(null != rela)
					{
						group.setIsJoinOfUser("1");//当前用户已加入当前群
						group.setIsOwner(rela.getIsGroupOwner());
					}
					else
					{
						group.setIsJoinOfUser("0");
						//判断当前用户是否已申请加群(status is null 是还没进行审核的加群申请)
						List<RelaApplyOfLbuyerorexpertAndGroup> applys = relaApplybuyerAndGroupService.
								getRelaApplyOfLbuyerorexpertAndGroupByCreatorAndStatus(userId,group.getId());
						if(null != applys && applys.size()>0)
						{
							group.setAlreadyApplyOfUser("1");
						}
						else
						{
							group.setAlreadyApplyOfUser("0");
						}
						
					}
					
					
				}
			}

			
		}
		
		
		map.put("flag", true);
		map.put("message", "获取成功");
		map.put("groupDtos", dtos);
		
		return map;
	}
	
	/**
	 * 获取当前群主管理的群
	* @Title: getGroupsOfOwner 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param userId
	* @param @param request
	* @param @param httpSession
	* @param @return    设定文件 
	* @author banna
	* @date 2017年4月24日 下午3:52:17 
	* @return Map<String,Object>    返回类型 
	* @throws
	 */
	@RequestMapping(value="/getGroupsOfOwner", method = RequestMethod.GET)
	public @ResponseBody Map<String,Object> getGroupsOfOwner(
			String userId,
			HttpServletRequest request,HttpSession httpSession)
	{
		 Map<String,Object> map = new HashMap<String, Object>();
		 List<LotteryGroupDTO> groupDtos = new ArrayList<LotteryGroupDTO>();
		 
		 LotterybuyerOrExpert manager = lotterybuyerOrExpertService.getLotterybuyerOrExpertById(userId);
		 List<LotteryGroup> groups = manager.getLotteryGroups();
		 
		 try
		 {
			 for (LotteryGroup lotteryGroup : groups) 
			 {
				 LotteryGroupDTO dto = new LotteryGroupDTO();
//				 BeanUtil.copyBeanProperties(dto, lotteryGroup);
				 dto = lotteryGroupService.toDTO(lotteryGroup);
				 groupDtos.add(dto);
			 }
			 
			 map.put("flag", true);
			 map.put("message", "获取成功");
			 map.put("groupDtos", groupDtos);
		 }
		 catch(Exception e)
		 {
			 logger.error("error:", e);
			 map.put("flag", false);
			 map.put("message", "获取失败");
		 }
		 
		 return map;
	}
	
	/**
	 * 获取当前用户加入的群
	* @Title: getGroupsOfUserjoins 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param UserId
	* @param @param request
	* @param @param httpSession
	* @param @return    设定文件 
	* @author banna
	* @date 2017年4月24日 下午3:00:47 
	* @return Map<String,Object>    返回类型 
	* @throws
	 */
	@RequestMapping(value="/getGroupsOfUserjoins", method = RequestMethod.GET)
	public @ResponseBody Map<String,Object> getGroupsOfUserjoins(
			String userId,
			HttpServletRequest request,HttpSession httpSession)
	{
		 Map<String,Object> map = new HashMap<String, Object>();
		 List<LotteryGroupDTO> groupDtos = new ArrayList<LotteryGroupDTO>();
		 LotterybuyerOrExpert user = lotterybuyerOrExpertService.getLotterybuyerOrExpertById(userId);
		 try
		 {
			 if(null != user)
			 {
				 List<RelaBindOfLbuyerorexpertAndGroup> relaGroups = relaBindbuyerAndGroupService.getRelaList(userId);
				 
				 for (RelaBindOfLbuyerorexpertAndGroup relaGroup : relaGroups)
				 {
					if(null != relaGroup.getLotteryGroup())
					{
						LotteryGroupDTO dto = new LotteryGroupDTO();
//						BeanUtil.copyBeanProperties(dto, relaGroup.getLotteryGroup());
						dto = lotteryGroupService.toDTO(relaGroup.getLotteryGroup());
						dto.setIsOwner(relaGroup.getIsGroupOwner());
						dto.setIsTop(relaGroup.getIsTop());
						groupDtos.add(dto);
					}
				 }
				 
				 map.put("flag", true);
				 map.put("message", "获取成功");
				 map.put("groupDtos", groupDtos);
			 }
		 }
		 catch(Exception e)
		 {
			 logger.error("error:", e);
			 map.put("flag", false);
			 map.put("message", "获取失败");
		 }
		
		 
		 
		 return map;
	}
	
	/**
	 * 申请加入群
	* @Title: applyJoinGroup 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param userId
	* @param @param groupId
	* @param @param applyMessage
	* @param @return    设定文件 
	* @author banna
	* @date 2017年5月9日 上午9:57:57 
	* @return ResultBean    返回类型 
	* @throws
	 */
	@RequestMapping(value="/applyJoinGroup", method = RequestMethod.GET)
	public @ResponseBody ResultBean applyJoinGroup(
			@RequestParam(value="userId",required=false) String userId,
			@RequestParam(value="groupId",required=false) String groupId,
			@RequestParam(value="applyMessage",required=false) String applyMessage)
	{
		ResultBean resultBean = new ResultBean();
		
		//建立用户和群的申请关系
		RelaApplyOfLbuyerorexpertAndGroup entity = new RelaApplyOfLbuyerorexpertAndGroup();
		LotterybuyerOrExpert user = null;
		if(null != userId)
		 user = lotterybuyerOrExpertService.getLotterybuyerOrExpertById(userId);
		
		LotteryGroup lotteryGroup = null;
		if(null != groupId)
			lotteryGroup = lotteryGroupService.getLotteryGroupById(groupId);
		
		entity.setId(UUID.randomUUID().toString());//生成主键id
		entity.setIsDeleted(Constants.IS_NOT_DELETED);
		entity.setLotterybuyerOrExpert(user);
		entity.setLotteryGroup(lotteryGroup);
		
		if(null != applyMessage &&!"".equals(applyMessage))//若申请信息不为空，则将申请信息放入
			entity.setApplyMessage(applyMessage);
		
		entity.setCreator(userId);
		entity.setCreateTime(new Timestamp(System.currentTimeMillis()));
		entity.setModify(userId);
		entity.setModifyTime(new Timestamp(System.currentTimeMillis()));
		entity.setApprovalUser(lotteryGroup.getLotteryBuyerOrExpert().getId());//添加群主id
		
		//保存用户的申请
		if(null != userId && null != groupId)
			relaApplybuyerAndGroupService.save(entity);
		
		//TODO:将申请信息推送给群主,推送给群主是approval
		LotterybuyerOrExpert groupOwner = lotteryGroup.getLotteryBuyerOrExpert();
		String[] tagsand = {groupOwner.getTelephone()};//推送给群主id，推送给群主审核
		PushController.sendPushWithCallback(tagsand, null, "1", "group");//推送给群主展示的是“1”
		
		return resultBean;
	}
	
	/**
	 * 群主审批加群申请
	* @Title: gOwnerApprovalApplys 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param userId
	* @param @param groupId
	* @param @param isPass
	* @param @param notPassMessage
	* @param @return    设定文件 
	* @author banna
	* @date 2017年5月9日 上午11:09:39 
	* @return ResultBean    返回类型 
	* @throws
	 */
	@RequestMapping(value="/gOwnerApprovalApplys", method = RequestMethod.GET)
	public @ResponseBody ResultBean gOwnerApprovalApplys(
			@RequestParam(value="userId",required=false) String userId,
			@RequestParam(value="groupId",required=false) String groupId,
			@RequestParam(value="groupOwnerId",required=false) String groupOwnerId,//群主id
			@RequestParam(value="isPass",required=false) String isPass,//1：通过0：不通过
			@RequestParam(value="notPassMessage",required=false) String notPassMessage)
	{
		ResultBean resultBean = new ResultBean();
		
		//##如果审批通过，执行将用户加入群的操作##
		
		//根据用户id和群id获取用户申请加入群的信息
		List<RelaApplyOfLbuyerorexpertAndGroup> entities = relaApplybuyerAndGroupService.
				getRelaApplyOfLbuyerorexpertAndGroupByUserIdAndGroupId(userId, groupId);
		
		RelaApplyOfLbuyerorexpertAndGroup entity = null;
		if(null != entities  && entities.size()>0)
		{
			entity = entities.get(0);
			entity.setStatus(isPass);//放置审核状态
			
			if("1".equals(isPass))
			{//审核通过，执行加群操作
				String[] joinUsers = {userId};//组合加群用户数组
				this.joinUserInGroup(joinUsers, groupId);
			}
			else
				if("0".equals(isPass))
				{//审核不通过，执行不通过信息的添加
					if(null != notPassMessage && !"".equals(notPassMessage))
					  entity.setNotPassMessage(notPassMessage);
					
				}
			
			entity.setModify(groupOwnerId);
			entity.setModifyTime(new Timestamp(System.currentTimeMillis()));
			
			relaApplybuyerAndGroupService.update(entity);
		}
		
		//TODO:将群主审核的结果推送给申请加群的用户,群主审核的tag是apply
		String[] tagsand = {entity.getLotterybuyerOrExpert().getTelephone()};//推送给申请加群的用户手机号
		PushController.sendPushWithCallback(tagsand, null, "0", "group");//推送给用户展示的是“0”
		
		resultBean.setFlag(true);
		resultBean.setMessage("审核成功");
		
		return resultBean;
	}
	
	/**
	 * 获取当前用户的申请加群的列表
	* @Title: getApplyGroupList 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param userId
	* @param @return    设定文件 
	* @author banna
	* @date 2017年5月9日 上午11:42:52 
	* @return Map<String,Object>    返回类型 
	* @throws
	 */
	@RequestMapping(value="/getApplyGroupList", method = RequestMethod.GET)
	public @ResponseBody Map<String , Object> getApplyGroupList(
			@RequestParam(value="userId",required=false) String userId)
	{
		Map<String , Object> map = new HashMap<String, Object>();
		
		try
		{
			List<RelaApplyOfLbuyerorexpertAndGroup> entities = relaApplybuyerAndGroupService.
					getRelaApplyOfLbuyerorexpertAndGroupByCreator(userId);
			
			List<RelaApplyOfLbuyerorexpertAndGroupDTO> dtos = relaApplybuyerAndGroupService.toDTOS(entities);
			
			map.put("applyList", dtos);
			map.put("flag", true);
		}
		catch(Exception e)
		{
			logger.error("error", e);
			map.put("flag", false);
		}
		
		return map;
	}
	
	/**
	 * 获取当前用户（群主）需要审核的申请加群列表
	* @Title: getApprovalList 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param userId
	* @param @return    设定文件 
	* @author banna
	* @date 2017年5月9日 上午11:47:45 
	* @return Map<String,Object>    返回类型 
	* @throws
	 */
	@RequestMapping(value="/getApprovalList", method = RequestMethod.GET)
	public @ResponseBody Map<String , Object> getApprovalList(
			@RequestParam(value="ownerId",required=false) String ownerId)
	{
		Map<String , Object> map = new HashMap<String, Object>();
		
		try
		{
			List<RelaApplyOfLbuyerorexpertAndGroup> entities = relaApplybuyerAndGroupService.
					getRelaApplyOfLbuyerorexpertAndGroupByApprovalUser(ownerId);
			
			List<RelaApplyOfLbuyerorexpertAndGroupDTO> dtos = relaApplybuyerAndGroupService.toDTOS(entities);
			
			map.put("approvalList", dtos);
			map.put("flag", true);
		}
		catch(Exception e)
		{
			logger.error("error", e);
			map.put("flag", false);
		}
		
		return map;
	}
	
	/**
	 * 根据群号获取群信息
	* @Title: getGroupByGroupnumber 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param groupNumber
	* @param @return    设定文件 
	* @author banna
	* @date 2017年5月9日 下午2:51:00 
	* @return LotteryGroupDTO    返回类型 
	* @throws
	 */
	@RequestMapping(value="/getGroupByGroupnumber", method = RequestMethod.GET)
	public @ResponseBody LotteryGroupDTO getGroupByGroupnumber(
			@RequestParam(value="groupNumber",required=false)  String groupNumber)
	{
		LotteryGroup group = lotteryGroupService.getLotteryGroupByGroupNumber(groupNumber);
		
		LotteryGroupDTO dto = lotteryGroupService.toDTO(group);
		
		return dto;
	}
	
	/**
	 * 向群中加入用户
	* @Title: joinUserInGroup 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param joinUsers
	* @param @param groupId
	* @param @param request
	* @param @param httpSession
	* @param @return    设定文件 
	* @author banna
	* @date 2017年4月24日 下午2:12:12 
	* @return ResultBean    返回类型 
	* @throws
	 */
	@RequestMapping(value="/joinUserInGroup", method = RequestMethod.GET)
	public @ResponseBody ResultBean joinUserInGroup(
			@RequestParam(value="joinUsers",required=false)  String[] joinUsers,
			@RequestParam(value="groupId",required=false) String groupId)
	{
		ResultBean resultBean = new ResultBean();
		
		//建立群和要加入用户的关联
		LotteryGroup group = lotteryGroupService.getLotteryGroupById(groupId);
		Integer nowMemberCount = group.getRelaBindOfLbuyerorexpertAndGroups().size();//获取当前群中的群成员人数
		Integer memberCount = group.getMemberCount();//获取群成员可以加入的人数
		
		//若要加入的人数和现在的人数的总和小于群可以加入的人数，则可以继续添加，否则无法添加
		int overplusMember= memberCount-nowMemberCount-joinUsers.length;//获取可以加入的人数
		if(overplusMember>=0)
		{
			LotterybuyerOrExpert user = null;
			for (String userId : joinUsers) 
			{
				user = lotterybuyerOrExpertService.getLotterybuyerOrExpertById(userId);
				RelaBindOfLbuyerorexpertAndGroup rela = new RelaBindOfLbuyerorexpertAndGroup();
				rela.setIsDeleted(Constants.IS_NOT_DELETED);
				rela.setIsReceive("1");
				rela.setIsTop("0");//是否置顶1：置顶 0：不置顶
				rela.setIsGroupOwner("0");//群成员
				rela.setLotterybuyerOrExpert(user);
				rela.setLotteryGroup(group);
				rela.setCreator(groupId);
				rela.setCreateTime(new Timestamp(System.currentTimeMillis()));
				rela.setModify(groupId);
				rela.setModifyTime(new Timestamp(System.currentTimeMillis()));

				//保存关联
				relaBindbuyerAndGroupService.save(rela);
			}
			
			//建立融云中群和用户的关系
			CodeSuccessResult result= rongyunImService.joinUserInGroup(joinUsers, groupId, group.getName());
			if(!OuterLotteryGroupController.SUCCESS_CODE.equals(result.getCode().toString()))
			{
				logger.error("融云群加入用户报错", result.getErrorMessage());
			}
			
			resultBean.setFlag(true);
			resultBean.setMessage("加入成功");
		}
		else
		{
			int couldJoin = joinUsers.length-(nowMemberCount+joinUsers.length-memberCount);
			resultBean.setFlag(false);
			resultBean.setMessage("群等级不够加入当前要求加入的人数，当前只可以加入:"+couldJoin+"人");
		}
		
		
		
		return resultBean;
	}
	
	/**
	 * 从群中移除用户(机器人用户不可以被删除，前台也要做校验)
	* @Title: quitUserFronGroup 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param quitUsers
	* @param @param groupId
	* @param @param request
	* @param @param httpSession
	* @param @return    设定文件 
	* @author banna
	* @date 2017年4月24日 下午2:12:02 
	* @return ResultBean    返回类型 
	* @throws
	 */
	@RequestMapping(value="/quitUserFronGroup", method = RequestMethod.GET)
	public @ResponseBody ResultBean quitUserFronGroup(
			@RequestParam(value="quitUsers",required=false)   String[] quitUsers,
			@RequestParam(value="groupId",required=false)   String groupId,
			HttpServletRequest request,HttpSession httpSession)
	{
		ResultBean resultBean = new ResultBean();
		
		//解除群和要加入用户的关联
		for (String userId : quitUsers) 
		{
			//根据用户id和群id获取关联关系
			RelaBindOfLbuyerorexpertAndGroup rela = relaBindbuyerAndGroupService.
					getRelaBindOfLbuyerorexpertAndGroupByUserIdAndGroupId(userId, groupId);
			LotterybuyerOrExpert user = lotterybuyerOrExpertService.getLotterybuyerOrExpertById(userId);
			//机器人用户不可以被删除
			if(null != rela && !"1".equals(user.getIsRobot()))
			{
				//删除关联
				rela.setLotterybuyerOrExpert(null);
				rela.setLotteryGroup(null);
				rela.setIsDeleted(Constants.IS_DELETED);
				relaBindbuyerAndGroupService.update(rela);
			}
			
		}
		
		//删除融云中群和用户的关系
		CodeSuccessResult result = rongyunImService.quitUserFronGroup(quitUsers, groupId);
		if(!OuterLotteryGroupController.SUCCESS_CODE.equals(result.getCode().toString()))
		{
			logger.error("融云执行用户退群时错误：", result.getErrorMessage());
		}
		
		resultBean.setFlag(true);
		resultBean.setMessage("退群成功");
		
		return resultBean;
	}
	
	/**
	 * 修改群信息
	* @Title: updateGroup 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param dto
	* @param @param request
	* @param @param httpSession
	* @param @return    设定文件 
	* @author banna
	* @date 2017年4月24日 下午2:18:17 
	* @return Map<String,Object>    返回类型 
	* @throws
	 */
	@RequestMapping(value="/updateGroup", method = RequestMethod.GET)
	public @ResponseBody Map<String,Object> updateGroup(
			LotteryGroupDTO dto,
			HttpServletRequest request,HttpSession httpSession)
	{
		Map<String,Object> map = new HashMap<String, Object>();
		
		try
		{
			//修改群新新信息（修改群名称）
			if(null != dto.getName() && !"".equals(dto.getName()))
			{
				LotteryGroup group = lotteryGroupService.getLotteryGroupById(dto.getId());
				group.setName(dto.getName());
				//更改群名称
				lotteryGroupService.update(group);
				map.put("flag", true);
				map.put("message", "更新群信息成功");
				map.put("group", BeanUtil.copyBeanProperties(dto, group));
				
				//刷新群的信息
				CodeSuccessResult result = rongyunImService.refreshGroup(dto.getId(), dto.getName());
				if(!OuterLotteryGroupController.SUCCESS_CODE.equals(result.getCode().toString()))
				{
					logger.error("融云刷新群信息时出错：", result.getErrorMessage());
				}
			}
			
			//TODO:群升级，群升级时才传递这个参数(升到几级，升2级传值2)
			if(null != dto.getUpLevel() && !"".equals(dto.getUpLevel()))
			{
				LotteryGroup group = lotteryGroupService.getLotteryGroupById(dto.getId());
				
				//获取上次群升级的记录，获取上次的等级
				LGroupLevel beforeLevel = lGroupLevelService.getLGroupLevelByID(group.getGroupLevel());//获取上一次等级的数据
				
				LGroupLevel afterLevel = lGroupLevelService.getLGroupLevelByID(dto.getUpLevel());//要升到级数的群等级数据
				//放置群升级记录表数据
				RelaGroupUpLevelRecord level = new RelaGroupUpLevelRecord();
				level.setAfterLevel(afterLevel);
				level.setBeforeLevel(beforeLevel);
				level.setCreateTime(new Timestamp(System.currentTimeMillis()));
				level.setCreator(group.getId());
				level.setIsDeleted(Constants.IS_NOT_DELETED);
				level.setLotteryGroup(group);
				level.setModifyTime(new Timestamp(System.currentTimeMillis()));
				level.setModify(group.getId());
				level.setOperator(dto.getOwnerId());
				relaGroupUpLevelService.save(level);//保存群等级记录表数据
				
				group.setGroupLevel(dto.getUpLevel());//放置当前群等级，也就是要升到的群等级数，eg：升到2级就传2
				group.setMemberCount(afterLevel.getMemberCount());//更新membercount
				
				//更改群人数
				lotteryGroupService.update(group);
				map.put("flag", true);
				map.put("message", "升级群成功");
				map.put("group", BeanUtil.copyBeanProperties(dto, group));
			}
			
		
			
		}
		catch(Exception e)
		{
			logger.error("error:", e);
			map.put("flag", false);
			map.put("message", "更新群信息失败");
		}
		
		return map;
	}
	
	/**
	 * 创建群
	* @Title: createGroup 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param dto
	* @param @param request
	* @param @param httpSession
	* @param @return    设定文件 
	* @author banna
	* @date 2017年4月24日 上午9:57:27 
	* @return Map<String,Object>    返回类型 
	* @throws
	 */
	@RequestMapping(value="/createGroup")
	public @ResponseBody Map<String,Object> createGroup(
			LotteryGroupDTO dto,
			HttpServletRequest request)
	{
		Map<String,Object> map = new HashMap<String, Object>();
		
		LotteryGroup entity = new LotteryGroup();
		//在服务器创建群信息
		try 
		{
			if(null != dto.getOwnerId() && !"".equals(dto.getOwnerId()))
			{
				BeanUtil.copyBeanProperties(entity, dto);
				entity.setId(UUID.randomUUID().toString());//生成id
				
				entity.setGroupNumber(lotteryGroupService.generateGroupNumber());//放置群号
				LotterybuyerOrExpert owner = lotterybuyerOrExpertService.
						getLotterybuyerOrExpertById(dto.getOwnerId());
				entity.setLotteryBuyerOrExpert(owner);//放置群与群主的关系
				
				//处理群头像
				Uploadfile uploadfile =null;
				if(null != dto.getTouXiangImg())
				{
					String newsUuid = UUID.randomUUID().toString();
					try {
							 uploadfile = uploadfileService.uploadFiles(dto.getTouXiangImg(),request,newsUuid);
						
					} catch (Exception e) {
						logger.error("error:", e);
					}
					entity.setTouXiang(uploadfile.getNewsUuid());
				}
				
				//TODO:创建群的同时创建群的机器人,如果区域彩种机器人已经存在，或者机器人加群数以及饱和，则要再创建机器人
				String robotUserId = lotterybuyerOrExpertService.
						createRobotUser(dto.getProvince(), dto.getCity(), dto.getLotteryType());
				
				entity.setGroupRobotID(robotUserId);
				
				//TODO:放置群等级
				String level1Id = "1";//等级1群的等级id
				entity.setMemberCount(20);//以及群
				entity.setGroupLevel(level1Id);
				
				entity.setIsDeleted(Constants.IS_NOT_DELETED);
				entity.setCreator(dto.getOwnerId());
				entity.setCreateTime(new Timestamp((System.currentTimeMillis())));
				entity.setModify(dto.getOwnerId());
				entity.setModifyTime(new Timestamp((System.currentTimeMillis())));
				
				//生成群二维码
				String logo = null;//内嵌logo图片，若群头像不为空，则嵌入群头像
				String uploadPath = "upload";
				String path = request.getSession().getServletContext().getRealPath(uploadPath); 
				if(null != uploadfile)
					logo = path+File.separator+uploadfile.getUploadRealName();
				
				String fileName = QRCodeUtil.encode(entity.getGroupNumber(), logo, path, true,entity.getGroupNumber());
				entity.setGroupQRImg(File.separator+uploadPath+File.separator+fileName);
				//保存群信息
				lotteryGroupService.save(entity);
				
				//放置群升级记录表数据
				RelaGroupUpLevelRecord level = new RelaGroupUpLevelRecord();
				LGroupLevel L1 = lGroupLevelService.getLGroupLevelByID(level1Id);//获取L1等级的实体数据
				level.setAfterLevel(L1);//一级群
				level.setBeforeLevel(null);
				level.setCreateTime(new Timestamp(System.currentTimeMillis()));
				level.setCreator(entity.getId());
				level.setIsDeleted(Constants.IS_NOT_DELETED);
				level.setLotteryGroup(entity);
				level.setModifyTime(new Timestamp(System.currentTimeMillis()));
				level.setModify(entity.getId());
				level.setOperator(dto.getOwnerId());
				relaGroupUpLevelService.save(level);//保存群等级记录表数据
				
				//2017-5-11ADD：建立群主和群的加入关系
				RelaBindOfLbuyerorexpertAndGroup rela = new RelaBindOfLbuyerorexpertAndGroup();
				rela.setIsDeleted(Constants.IS_NOT_DELETED);
				rela.setIsReceive("1");
				rela.setIsTop("0");//是否置顶1：置顶 0：不置顶
				rela.setIsGroupOwner("1");//群主
				rela.setLotterybuyerOrExpert(owner);
				rela.setLotteryGroup(entity);
				rela.setCreator(dto.getOwnerId());
				rela.setCreateTime(new Timestamp(System.currentTimeMillis()));
				rela.setModify(dto.getOwnerId());
				rela.setModifyTime(new Timestamp(System.currentTimeMillis()));
				//保存关联
				relaBindbuyerAndGroupService.save(rela);
				
				//在融云创建群信息
				String[] joinUserId = {dto.getOwnerId(),robotUserId};//群主id加入要加入群的数组中,机器人加入群组中
				CodeSuccessResult result = rongyunImService.createGroup(joinUserId, entity.getId(), entity.getName());
				
				if(!SUCCESS_CODE.equals(result.getCode().toString()))
				{//若创建失败
					map.put("messsage", result.getErrorMessage());//创建失败返回融云端群创建失败信息
					logger.error("createGroup error:", result.getErrorMessage());
					map.put("flag", false);
				}
				else
				{
					map.put("group", lotteryGroupService.toDTO(entity));//返回创建成功的群信息
					map.put("message", "创建成功");
					map.put("flag", true);
					
					//TODO:1.创建成功后，将当前群主的建群卡个数减1
					
					
				}
				
			}
			
			
			
			
			
		} catch (Exception e) 
		{
			logger.error("error:", e);
			map.put("message", "创建失败");
			map.put("flag", false);
		}
		
		
		
		
		return map;
	}
	
	/**
	 * 查找群
	* @Title: findGroup 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param dto
	* @param @param request
	* @param @param httpSession
	* @param @return    设定文件 
	* @author banna
	* @date 2017年4月25日 上午10:59:27 
	* @return Map<String,Object>    返回类型 
	* @throws
	 */
	@RequestMapping(value="/findGroup", method = RequestMethod.GET)
	public @ResponseBody Map<String,Object> findGroup(
			LotteryGroupDTO dto,
			HttpServletRequest request,HttpSession httpSession)
	{
		Map<String,Object> map = new HashMap<String, Object>();
		
		
		
		return map;
	}
	
	/**
	 * 用户操作置顶/取消置顶群操作
	* @Title: updateGroupIsTop 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param isTop
	* @param @param userId
	* @param @return    设定文件 
	* @author banna
	* @date 2017年5月11日 上午9:53:02 
	* @return Map<String,Object>    返回类型 
	* @throws
	 */
	@RequestMapping(value="/updateGroupIsTop", method = RequestMethod.GET)
	public @ResponseBody Map<String,Object> updateGroupIsTop(
			@RequestParam(value="isTop",required=false)  String isTop,
			@RequestParam(value="userId",required=false) String userId,
			@RequestParam(value="groupId",required=false) String groupId)
	{
		Map<String,Object> map = new HashMap<String, Object>();
		
		//更新置顶状态
		RelaBindOfLbuyerorexpertAndGroup entity = relaBindbuyerAndGroupService.
				getRelaBindOfLbuyerorexpertAndGroupByUserIdAndGroupId(userId, groupId);
		entity.setIsTop(isTop);
		entity.setModifyTime(new Timestamp(System.currentTimeMillis()));
		entity.setModify(userId);
		relaBindbuyerAndGroupService.update(entity);
		
		//获取置顶后的群列表
		List<LotteryGroupDTO> dtos = new ArrayList<LotteryGroupDTO>();
		 List<RelaBindOfLbuyerorexpertAndGroup> relaGroups = relaBindbuyerAndGroupService.getRelaList(userId);
		 for (RelaBindOfLbuyerorexpertAndGroup relaGroup : relaGroups)
		 {//将排序关系转换为群信息
			if(null != relaGroup.getLotteryGroup())
			{
				LotteryGroupDTO dto = new LotteryGroupDTO();
				dto = lotteryGroupService.toDTO(relaGroup.getLotteryGroup());
				dto.setIsOwner(relaGroup.getIsGroupOwner());
				dto.setIsTop(relaGroup.getIsTop());
				dtos.add(dto);
			}
		 }
		 
		 map.put("flag", true);
		 map.put("groupDtos", dtos);
		
		
		return map;
	}
	
	
	
	
	
}
