package com.example.cae.scheduler.infrastructure.persistence.mapper;

import com.example.cae.scheduler.infrastructure.persistence.entity.NodeReservationPO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NodeReservationMapper {
	@Select("SELECT id, node_id AS nodeId, task_id AS taskId, status, created_at AS createdAt, updated_at AS updatedAt, released_at AS releasedAt FROM node_reservation WHERE node_id = #{nodeId} AND task_id = #{taskId} LIMIT 1 FOR UPDATE")
	NodeReservationPO selectByNodeIdAndTaskIdForUpdate(@Param("nodeId") Long nodeId, @Param("taskId") Long taskId);

	@Options(useGeneratedKeys = true, keyProperty = "id")
	@Insert("INSERT INTO node_reservation(node_id, task_id, status, released_at) VALUES(#{nodeId}, #{taskId}, #{status}, #{releasedAt})")
	int insert(NodeReservationPO po);

	@Update("UPDATE node_reservation SET status = #{status}, released_at = #{releasedAt} WHERE id = #{id}")
	int updateById(NodeReservationPO po);

	@Select("SELECT COUNT(1) FROM node_reservation WHERE node_id = #{nodeId} AND status = 'RESERVED'")
	int countReservedByNodeId(@Param("nodeId") Long nodeId);
}
