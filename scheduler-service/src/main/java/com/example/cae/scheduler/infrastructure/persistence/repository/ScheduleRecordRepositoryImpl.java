package com.example.cae.scheduler.infrastructure.persistence.repository;

import com.example.cae.common.response.PageResult;
import com.example.cae.scheduler.application.assembler.ScheduleAssembler;
import com.example.cae.scheduler.domain.model.ScheduleRecord;
import com.example.cae.scheduler.domain.repository.ScheduleRecordRepository;
import com.example.cae.scheduler.infrastructure.persistence.mapper.ScheduleRecordMapper;
import com.example.cae.scheduler.interfaces.request.SchedulePageQueryRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ScheduleRecordRepositoryImpl implements ScheduleRecordRepository {
	private final ScheduleRecordMapper scheduleRecordMapper;

	public ScheduleRecordRepositoryImpl(ScheduleRecordMapper scheduleRecordMapper) {
		this.scheduleRecordMapper = scheduleRecordMapper;
	}

	@Override
	public void save(ScheduleRecord record) {
		scheduleRecordMapper.insert(ScheduleAssembler.toPO(record));
	}

	@Override
	public PageResult<ScheduleRecord> page(SchedulePageQueryRequest request, long offset, int pageSize) {
		long total = scheduleRecordMapper.countPage(request);
		List<ScheduleRecord> records = scheduleRecordMapper.selectPage(request, offset, pageSize).stream().map(ScheduleAssembler::fromPO).toList();
		int pageNum = request == null || request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
		return PageResult.of(total, pageNum, pageSize, records);
	}

	@Override
	public List<ScheduleRecord> listByTaskId(Long taskId) {
		return scheduleRecordMapper.selectByTaskId(taskId).stream().map(ScheduleAssembler::fromPO).toList();
	}
}
