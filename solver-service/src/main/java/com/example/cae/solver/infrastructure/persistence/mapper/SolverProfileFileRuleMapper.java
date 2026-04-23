package com.example.cae.solver.infrastructure.persistence.mapper;

import com.example.cae.solver.infrastructure.persistence.entity.SolverProfileFileRulePO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SolverProfileFileRuleMapper {
	@Select("SELECT id, profile_id AS profileId, file_key AS fileKey, path_pattern AS pathPattern, file_name_pattern AS fileNamePattern, file_type AS fileType, required_flag AS requiredFlag, sort_order AS sortOrder, rule_json AS ruleJson, description FROM solver_profile_file_rule WHERE id = #{id} LIMIT 1")
	SolverProfileFileRulePO selectById(Long id);

	@Options(useGeneratedKeys = true, keyProperty = "id")
	@Insert("INSERT INTO solver_profile_file_rule(profile_id, file_key, path_pattern, file_name_pattern, file_type, required_flag, sort_order, rule_json, description) VALUES(#{profileId}, #{fileKey}, #{pathPattern}, #{fileNamePattern}, #{fileType}, #{requiredFlag}, #{sortOrder}, #{ruleJson}, #{description})")
	int insert(SolverProfileFileRulePO po);

	@Update("UPDATE solver_profile_file_rule SET path_pattern = #{pathPattern}, file_name_pattern = #{fileNamePattern}, file_type = #{fileType}, required_flag = #{requiredFlag}, sort_order = #{sortOrder}, rule_json = #{ruleJson}, description = #{description} WHERE id = #{id}")
	int updateById(SolverProfileFileRulePO po);

	@Delete("DELETE FROM solver_profile_file_rule WHERE id = #{ruleId}")
	int deleteById(Long ruleId);

	@Select("SELECT id, profile_id AS profileId, file_key AS fileKey, path_pattern AS pathPattern, file_name_pattern AS fileNamePattern, file_type AS fileType, required_flag AS requiredFlag, sort_order AS sortOrder, rule_json AS ruleJson, description FROM solver_profile_file_rule WHERE profile_id = #{profileId} ORDER BY sort_order ASC, id ASC")
	List<SolverProfileFileRulePO> selectByProfileId(Long profileId);
}
