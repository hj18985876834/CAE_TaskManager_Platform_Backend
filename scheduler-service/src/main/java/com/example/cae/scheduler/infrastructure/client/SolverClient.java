package com.example.cae.scheduler.infrastructure.client;

public interface SolverClient {
	SolverMeta getSolverMeta(Long solverId);

	ProfileMeta getProfileMeta(Long profileId);

	final class SolverMeta {
		private Long solverId;
		private String solverCode;
		private String solverName;
		private Integer enabled;

		public Long getSolverId() {
			return solverId;
		}

		public void setSolverId(Long solverId) {
			this.solverId = solverId;
		}

		public String getSolverCode() {
			return solverCode;
		}

		public void setSolverCode(String solverCode) {
			this.solverCode = solverCode;
		}

		public String getSolverName() {
			return solverName;
		}

		public void setSolverName(String solverName) {
			this.solverName = solverName;
		}

		public Integer getEnabled() {
			return enabled;
		}

		public void setEnabled(Integer enabled) {
			this.enabled = enabled;
		}

		public boolean isEnabled() {
			return Integer.valueOf(1).equals(enabled);
		}
	}

	final class ProfileMeta {
		private Long profileId;
		private Long solverId;
		private String profileCode;
		private String profileName;
		private Integer enabled;

		public Long getProfileId() {
			return profileId;
		}

		public void setProfileId(Long profileId) {
			this.profileId = profileId;
		}

		public Long getSolverId() {
			return solverId;
		}

		public void setSolverId(Long solverId) {
			this.solverId = solverId;
		}

		public String getProfileCode() {
			return profileCode;
		}

		public void setProfileCode(String profileCode) {
			this.profileCode = profileCode;
		}

		public String getProfileName() {
			return profileName;
		}

		public void setProfileName(String profileName) {
			this.profileName = profileName;
		}

		public Integer getEnabled() {
			return enabled;
		}

		public void setEnabled(Integer enabled) {
			this.enabled = enabled;
		}

		public boolean isEnabled() {
			return Integer.valueOf(1).equals(enabled);
		}
	}
}
