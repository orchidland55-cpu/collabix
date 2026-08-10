package com.trio.backend.repository;

import com.trio.backend.entity.TeamMember;
import com.trio.backend.entity.User;
import com.trio.backend.entity.ids.TeamMemberId;
import com.trio.backend.enums.WorkspaceMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamMemberRepository extends JpaRepository<TeamMember, TeamMemberId> {

    /**
     * Resorteves all the members of a team.
     *
     * @param teamId the ID of the team
     * @return list of members de the team
     */
    List<TeamMember> findAllByTeam_Id(UUID teamId);

    /**
     * Resorteves all the teams of a user.
     *
     * @param userId the ID of the user
     * @return list of members of team de the user
     */
    List<TeamMember> findAllByUser_Id(UUID userId);

    /**
     * Resorteves a member of team par its ID of team et its ID of user.
     *
     * @param teamId the ID of the team
     * @param userId the ID of the user
     * @return Optional containing the member si found
     */
    Optional<TeamMember> findByTeamMemberId_TeamIdAndTeamMemberId_UserId(UUID teamId, UUID userId);

    /**
     * Verifies si un user is a member of a team.
     *
     * @param teamId the ID of the team
     * @param userId the ID of the user
     * @return true si the user is a member, false sinon
     */
    boolean existsByTeamMemberId_TeamIdAndTeamMemberId_UserId(UUID teamId, UUID userId);

    /**
     * Resorteves the namebre de members in a team.
     *
     * @param teamId the ID of the team
     * @return the namebre de members
     */
    long countByTeam_Id(UUID teamId);

    // ==================== DASHBOARD-SPECIFIC QUERIES ====================

    /**
     * Resorteves all the members of a team with loading de the user (user).
     *
     * <p>Utilise {@code JOIN FETCH} to avoid le N+1 sur {@code teamMember.user}
     * lors du mapping vers les widgets du Department Dashboard.</p>
     *
     * @param teamId the ID of the team
     * @return list of members de the team avec user loaded
     */
    @Query("""
            SELECT tm FROM TeamMember tm
            JOIN FETCH tm.user
            WHERE tm.teamMemberId.teamId = :teamId
            """)
    List<TeamMember> findAllByTeam_IdWithUser(@Param("teamId") UUID teamId);

    /**
     * Resorteves all the members of team of a department with loading
     * de the user (user) et de the team (team).
     *
     * <p>Utilise {@code JOIN FETCH} to avoid le N+1 sur {@code teamMember.user}
     * et {@code teamMember.team} lors du mapping vers les widgets du dashboard
     * department. Le scope est validated par the department.</p>
     *
     * @param departmentId the ID of the department
     * @return list of members of team of the department avec user et team loadeds
     */
    @Query("""
            SELECT tm FROM TeamMember tm
            JOIN FETCH tm.user
            JOIN FETCH tm.team
            WHERE tm.team.department.id = :departmentId
            """)
    List<TeamMember> findAllByDepartmentIdWithUserAndTeam(@Param("departmentId") UUID departmentId);

    // ==================== REPORTING-SPECIFIC QUERIES ====================

    /**
     * Counts the namebre de members of team in a department.
     *
     * @param departmentId the ID of the department
     * @return the namebre de members of team in the department
     */
    @Query("""
            SELECT COUNT(tm) FROM TeamMember tm
            WHERE tm.team.department.id = :departmentId
            """)
    long countByDepartmentId(@Param("departmentId") UUID departmentId);

    // ==================== DASHBOARD GENERAL QUERIES ====================

    /**
     * Counts the namebre total de members of team in a workspace.
     *
     * <p>Useful pour le calcul de la mediumne de members par team
     * dans le Workspace Dashboard.</p>
     *
     * @param workspaceId the ID of the workspace
     * @return the namebre total de members of team in the workspace
     */
    @Query("""
            SELECT COUNT(tm) FROM TeamMember tm
            WHERE tm.team.department.workspace.id = :workspaceId
            """)
    long countByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    @Modifying
    @Query("DELETE FROM TeamMember tm WHERE tm.user = :user")
    void deleteAllByUser(@Param("user") User user);

    /**
     * Permanently removes all memberships of a team.
     *
     * <p>Team memberships are genuinely owned by the team and must be removed
     * before the team row itself is deleted.</p>
     *
     * @param teamId the ID of the team
     */
    @Modifying
    @Query("DELETE FROM TeamMember tm WHERE tm.teamMemberId.teamId = :teamId")
    void deleteAllByTeamId(@Param("teamId") UUID teamId);
}
