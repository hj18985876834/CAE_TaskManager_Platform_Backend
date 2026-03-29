package com.example.cae.solver.domain.repository;

import com.example.cae.common.response.PageResult;
import com.example.cae.solver.domain.model.SolverTaskProfile;
import com.example.cae.solver.interfaces.request.ProfilePageQueryRequest;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository {
	Optional<SolverTaskProfile> findById(Long profileId);

	Optional<SolverTaskProfile> findBySolverIdAndProfileCode(Long solverId, String profileCode);

	void save(SolverTaskProfile profile);

	void update(SolverTaskProfile profile);

	PageResult<SolverTaskProfile> page(ProfilePageQueryRequest request, long offset, long pageSize);

	long count(ProfilePageQueryRequest request);

	List<SolverTaskProfile> listEnabledBySolverId(Long solverId);
	List<SolverTaskProfile> listBySolverId(Long solverId);
}

