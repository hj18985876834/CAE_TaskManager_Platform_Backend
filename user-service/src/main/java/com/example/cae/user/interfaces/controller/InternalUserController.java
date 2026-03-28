package com.example.cae.user.interfaces.controller;

import com.example.cae.common.response.Result;
import com.example.cae.user.application.facade.UserFacade;
import com.example.cae.user.interfaces.response.InternalUserBasicResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
public class InternalUserController {
	private final UserFacade userFacade;

	public InternalUserController(UserFacade userFacade) {
		this.userFacade = userFacade;
	}

	@GetMapping("/{id}")
	public Result<InternalUserBasicResponse> getById(@PathVariable("id") Long id) {
		return Result.success(userFacade.getInternalById(id));
	}
}
