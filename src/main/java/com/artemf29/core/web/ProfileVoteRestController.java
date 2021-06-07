package com.artemf29.core.web;

import com.artemf29.core.AuthorizedUser;
import com.artemf29.core.model.Vote;
import com.artemf29.core.repository.RestaurantRepository;
import com.artemf29.core.repository.UserRepository;
import com.artemf29.core.repository.VoteRepository;
import com.artemf29.core.to.VoteTo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

import static com.artemf29.core.util.UrlUtil.PROFILE_VOTE_URL;
import static com.artemf29.core.util.ValidationUtil.assureIdConsistent;
import static com.artemf29.core.util.ValidationUtil.checkNotFoundWithId;
import static com.artemf29.core.util.VoteUtil.createTo;
import static com.artemf29.core.util.VoteUtil.reVotingPermission;

@RestController
@RequestMapping(value = PROFILE_VOTE_URL, produces = MediaType.APPLICATION_JSON_VALUE)
public class ProfileVoteRestController {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected VoteRepository voteRepository;
    protected UserRepository userRepository;
    protected RestaurantRepository restaurantRepository;

    public ProfileVoteRestController(VoteRepository voteRepository, UserRepository userRepository, RestaurantRepository restaurantRepository) {
        this.voteRepository = voteRepository;
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @GetMapping("/")
    public ResponseEntity<VoteTo> get(@AuthenticationPrincipal AuthorizedUser authUser) {
        log.info("get for User{}", authUser.getId());
        return ResponseEntity.of(Optional.of(createTo(voteRepository.getByDate(LocalDate.now(), authUser.getId()).get())));
    }

    @PutMapping(value = "/{id}")
    public void update(@AuthenticationPrincipal AuthorizedUser authUser, @PathVariable int id, @RequestParam int restId) {
        reVotingPermission();
        int userId = authUser.getId();
        log.info("update vote for user {} by id restaurant {}", userId, restId);
        Vote vote = new Vote(id, LocalDate.now());
        assureIdConsistent(vote, id);
        checkNotFoundWithId(voteRepository.getById(id, userId), "Vote id=" + id + " doesn't belong to user id=" + userId);
        checkNotFoundWithId(restaurantRepository.findById(restId), "Restaurant id=" + restId);
        vote.setUser(userRepository.getOne(userId));
        vote.setRestaurant(restaurantRepository.getOne(restId));
        voteRepository.save(vote);
    }

    @PostMapping
    public ResponseEntity<Vote> createWithLocation(@AuthenticationPrincipal AuthorizedUser authUser, @RequestParam int restId) {
        int userId = authUser.getId();
        log.info("create vote for user {} by id restaurant {}", userId, restId);
        Vote vote = new Vote(null, LocalDate.now());
        checkNotFoundWithId(restaurantRepository.findById(restId), "Restaurant id=" + restId);
        vote.setUser(userRepository.getOne(userId));
        vote.setRestaurant(restaurantRepository.getOne(restId));
        return ResponseEntity.ok().body(voteRepository.save(vote));
    }
}