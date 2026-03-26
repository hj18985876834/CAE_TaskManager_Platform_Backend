package com.example.cae.solver.infrastructure.persistence.repository;

import com.example.cae.common.response.PageResult;
import com.example.cae.solver.application.assembler.ProfileAssembler;
import com.example.cae.solver.domain.model.SolverTaskProfile;
import com.example.cae.solver.domain.repository.ProfileRepository;
import com.example.cae.solver.infrastructure.persistence.mapper.SolverTaskProfileMapper;
import com.example.cae.solver.interfaces.request.ProfilePageQueryRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProfileRepositoryImpl implements ProfileRepository {
	private final SolverTaskProfileMapper solverTaskProfileMapper;

	public ProfileRepositoryImpl(SolverTaskProfileMapper solverTaskProfileMapper) {
		this.solverTaskProfileMapper = solverTaskProfileMapper;
	}

	@Override
	public Optional<SolverTaskProfile> findById(Long profileId) {
		return Optional.ofNullable(solverTaskProfileMapper.selectById(profileId)).map(ProfileAssembler::fromPO);
	}

	@Override
	public Optional<SolverTaskProfile> findBySolverIdAndProfileCode(Long solverId, String profileCode) {
		return Optional.ofNullable(solverTaskProfileMapper.selectBySolverIdAndProfileCode(solverId, profileCode)).map(ProfileAssembler::fromPO);
	}

	@Override
	public void save(SolverTaskProfile profile) {
		solverTaskProfileMapper.insert(ProfileAssembler.toPO(profile));
	}

	@Override
	public void update(SolverTaskProfile profile) {
		solverTaskProfileMapper.updateById(ProfileAssembler.toPO(profile));
	}

	@Override
	public PageResult<SolverTaskProfile> page(ProfilePageQueryRequest request, long offset, long pageSize) {
		List<SolverTaskProfile> records = solverTaskProfileMapper.selectPage(request, offset, pageSize)
				.stream()
				.map(ProfileAssembler::fromPO)
				.toList();
		long pageNum = request == null || request.getPageNum() == null ? 1L : request.getPageNum();
		return PageResult.of(count(request), pageNum, pageSize, records);
	}

	@Override
	public long count(ProfilePageQueryRequest request) {
		return solverTaskProfileMapper.count(request);
	}

	@Override
	public List<SolverTaskProfile> listEnabledBySolverId(Long solverId) {
		return solverTaskProfileMapper.selectEnabledBySolverId(solverId).stream().map(ProfileAssembler::fromPO).toList();
	}
}
