package com.example.cae.solver.application.service;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.common.response.PageResult;
import com.example.cae.solver.application.assembler.ProfileAssembler;
import com.example.cae.solver.application.assembler.SolverAssembler;
import com.example.cae.solver.domain.model.SolverDefinition;
import com.example.cae.solver.domain.repository.ProfileRepository;
import com.example.cae.solver.domain.repository.SolverRepository;
import com.example.cae.solver.domain.service.SolverDomainService;
import com.example.cae.solver.interfaces.request.CreateSolverRequest;
import com.example.cae.solver.interfaces.request.SolverPageQueryRequest;
import com.example.cae.solver.interfaces.request.UpdateSolverRequest;
import com.example.cae.solver.interfaces.request.UpdateSolverStatusRequest;
import com.example.cae.solver.interfaces.response.SolverProfileOptionResponse;
import com.example.cae.solver.interfaces.response.SolverCreateResponse;
import com.example.cae.solver.interfaces.response.SolverDetailResponse;
import com.example.cae.solver.interfaces.response.SolverListItemResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SolverAppService {
	private final SolverRepository solverRepository;
	private final ProfileRepository profileRepository;
	private final SolverDomainService solverDomainService;

	public SolverAppService(SolverRepository solverRepository, ProfileRepository profileRepository, SolverDomainService solverDomainService) {
		this.solverRepository = solverRepository;
		this.profileRepository = profileRepository;
		this.solverDomainService = solverDomainService;
	}

	public PageResult<SolverListItemResponse> pageSolvers(SolverPageQueryRequest request) {
		normalizePageQuery(request);
		int pageNum = request == null || request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
		int pageSize = request == null || request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();
		long offset = (long) (pageNum - 1) * pageSize;

		PageResult<SolverDefinition> page = solverRepository.page(request, offset, pageSize);
		List<SolverListItemResponse> records = page.getRecords().stream().map(SolverAssembler::toListItemResponse).toList();
		return PageResult.of(page.getTotal(), pageNum, pageSize, records);
	}

	public SolverDetailResponse getSolverDetail(Long solverId) {
		SolverDefinition solver = solverRepository.findById(solverId).orElseThrow(() -> new BizException(ErrorCodeConstants.SOLVER_NOT_FOUND, "solver not found"));
		return SolverAssembler.toDetailResponse(solver);
	}

	public SolverCreateResponse createSolver(CreateSolverRequest request) {
		solverDomainService.checkSolverCodeUnique(request.getSolverCode());
		SolverDefinition solver = SolverAssembler.toSolver(request);
		if (request.getEnabled() != null && request.getEnabled() == 0) {
			solver.disable();
		} else {
			solver.enable();
		}
		solverRepository.save(solver);
		return SolverAssembler.toCreateResponse(solver);
	}

	public void updateSolver(Long solverId, UpdateSolverRequest request) {
		SolverDefinition solver = solverRepository.findById(solverId).orElseThrow(() -> new BizException(ErrorCodeConstants.SOLVER_NOT_FOUND, "solver not found"));
		solver.setSolverName(request.getSolverName());
		solver.setVersion(request.getVersion());
		solver.setExecMode(request.getExecMode());
		solver.setExecPath(request.getExecPath());
		solver.setDescription(request.getDescription());
		solverRepository.update(solver);
	}

	public void updateSolverStatus(Long solverId, UpdateSolverStatusRequest request) {
		SolverDefinition solver = solverRepository.findById(solverId).orElseThrow(() -> new BizException(ErrorCodeConstants.SOLVER_NOT_FOUND, "solver not found"));
		if (request != null && request.getEnabled() != null && request.getEnabled() == 1) {
			solver.enable();
		} else {
			solver.disable();
		}
		solverRepository.update(solver);
	}

	public List<SolverProfileOptionResponse> getSolverProfileOptions(Long solverId) {
		return profileRepository.listEnabledBySolverId(solverId).stream().map(ProfileAssembler::toProfileOptionResponse).toList();
	}

	private void normalizePageQuery(SolverPageQueryRequest request) {
		if (request == null) {
			return;
		}
		request.setSolverCode(trimToNull(request.getSolverCode()));
		request.setSolverName(trimToNull(request.getSolverName()));
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
