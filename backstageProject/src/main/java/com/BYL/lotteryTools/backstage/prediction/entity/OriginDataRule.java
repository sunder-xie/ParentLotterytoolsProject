package com.BYL.lotteryTools.backstage.prediction.entity;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import com.BYL.lotteryTools.common.entity.BaseEntity;

/**
 * 
* @Description: TODO(11选5/12选5源码规则实体类)开奖号码是5个的可以使用 
* @author banna
* @date 2017年3月23日 下午3:32:10
 */
@Entity
@Table(name="T_LT_ORIGINDATA_RULE")
public class OriginDataRule extends BaseEntity
{
	@Id
	@Column(name="ID", nullable=false, length=45)
	@GenericGenerator(name="idGenerator", strategy="uuid")//uuid由机器生成的主键
	@GeneratedValue(generator="idGenerator")	
	private String id;
	
	@Column(name="RULE_NAME", length=45)
	private String ruleName;//源码规则名称
	
	@Column(name="TYPE", length=45)
	private String type;//:0：当前开奖号码1:关联期号方式2.期号方式
	
	
	@Column(name="LOCATION_OR_CONTAIN", length=45)
	private String locationOrContain;//0:定位1：包含
	
	@Column(name="CI_LOCATION_NUMBER", length=45)
	private String ciLocationNumber;//当前期需要定位的位置，若要取第一位则是1，若要取第一位和第二位开奖号码，则要填写1,2
	
	
	@Column(name="CI_RULE_FILED", length=45)
	private String ciRuleFiled;//筛选条件字段，同彩种维护中需要计算字段的维护方式，筛选的字段用","间隔
	
	@Column(name="LI_LOCATION_NUMBER", length=45)
	private String liLocationNumber;//上期需要定位的位置，若要取第一位则是1，若要取第一位和第二位开奖号码，则要填写1,2
	
	
	@Column(name="LI_RULE_FILED", length=45)
	private String liRuleFiled;//上期筛选条件字段，同彩种维护中需要计算字段的维护方式，筛选的字段用","间隔
	
	@Column(name="CYCLE", length=45)
	private String cycle;//周期，间隔周期获取同样期号的开奖数据
	
	@OneToMany(mappedBy="originDataRule",fetch=FetchType.LAZY)
	private List<BasePredictionType> basePredictionTypes;
	
	

	public String getCiLocationNumber() {
		return ciLocationNumber;
	}

	public void setCiLocationNumber(String ciLocationNumber) {
		this.ciLocationNumber = ciLocationNumber;
	}

	public String getCiRuleFiled() {
		return ciRuleFiled;
	}

	public void setCiRuleFiled(String ciRuleFiled) {
		this.ciRuleFiled = ciRuleFiled;
	}

	public String getLiLocationNumber() {
		return liLocationNumber;
	}

	public void setLiLocationNumber(String liLocationNumber) {
		this.liLocationNumber = liLocationNumber;
	}

	public String getLiRuleFiled() {
		return liRuleFiled;
	}

	public void setLiRuleFiled(String liRuleFiled) {
		this.liRuleFiled = liRuleFiled;
	}

	public List<BasePredictionType> getBasePredictionTypes() {
		return basePredictionTypes;
	}

	public void setBasePredictionTypes(List<BasePredictionType> basePredictionTypes) {
		this.basePredictionTypes = basePredictionTypes;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRuleName() {
		return ruleName;
	}

	public void setRuleName(String ruleName) {
		this.ruleName = ruleName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getLocationOrContain() {
		return locationOrContain;
	}

	public void setLocationOrContain(String locationOrContain) {
		this.locationOrContain = locationOrContain;
	}


	public String getCycle() {
		return cycle;
	}

	public void setCycle(String cycle) {
		this.cycle = cycle;
	}
	
	
	
	
	
	
}
