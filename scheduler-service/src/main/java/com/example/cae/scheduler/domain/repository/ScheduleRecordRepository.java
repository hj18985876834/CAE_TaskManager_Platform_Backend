package com.example.cae.scheduler.domain.repository;

import com.example.cae.common.response.PageResult;
import com.example.cae.scheduler.domain.model.ScheduleRecord;
import com.example.cae.scheduler.interfaces.request.SchedulePageQueryRequest;

public interface ScheduleRecordRepository {
	void save(ScheduleRecord record);

	PageResult<ScheduleRecord> page(SchedulePageQueryRequest request, long offset, int pageSize);
}
