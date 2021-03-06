package com.artemf29.core.web.vote;

import com.artemf29.core.model.Vote;
import com.artemf29.core.repository.VoteRepository;
import com.artemf29.core.util.VoteUtil;
import com.artemf29.core.util.json.JsonUtil;
import com.artemf29.core.web.AbstractControllerTest;
import com.artemf29.core.web.ExceptionInfoHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.artemf29.core.TestUtil.readFromJson;
import static com.artemf29.core.TestUtil.userHttpBasic;
import static com.artemf29.core.testdata.RestaurantTestDataUtils.RESTAURANT_1_ID;
import static com.artemf29.core.testdata.RestaurantTestDataUtils.RESTAURANT_2_ID;
import static com.artemf29.core.testdata.RestaurantTestDataUtils.RESTAURANT_3_ID;
import static com.artemf29.core.testdata.RestaurantTestDataUtils.restaurant1;
import static com.artemf29.core.testdata.RestaurantTestDataUtils.restaurant2;
import static com.artemf29.core.testdata.UserTestDataUtils.ADMIN_ID;
import static com.artemf29.core.testdata.UserTestDataUtils.USER_ID;
import static com.artemf29.core.testdata.UserTestDataUtils.admin;
import static com.artemf29.core.testdata.UserTestDataUtils.user;
import static com.artemf29.core.testdata.VoteTestDataUtils.*;
import static com.artemf29.core.util.UrlUtil.PROFILE_VOTE_URL;
import static java.time.LocalTime.now;
import static java.time.LocalTime.of;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileVoteRestControllerTest extends AbstractControllerTest {
    private static final String REST_URL = PROFILE_VOTE_URL + '/';

    @Autowired
    private VoteRepository voteRepository;

    @Test
    void getToday() throws Exception {
        perform(MockMvcRequestBuilders.get(REST_URL)
                .with(userHttpBasic(admin)))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(VOTE_TO_MATCHER.contentJson(VoteUtil.createTo(vote1)));
    }

    @Test
    void getUnAuth() throws Exception {
        perform(MockMvcRequestBuilders.get(REST_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createUnAuth() throws Exception {
        perform(MockMvcRequestBuilders.post(REST_URL)
                .param("restId", Integer.toString(RESTAURANT_2_ID)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void update() throws Exception { //if local time is before 11am
        Vote updated = getUpdated();
        if (now().isAfter(of(11, 0))) {
            perform(MockMvcRequestBuilders.put(REST_URL + VOTE_1_ID)
                    .param("restId", Integer.toString(RESTAURANT_3_ID))
                    .with(userHttpBasic(admin)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(content().string(containsString(ExceptionInfoHandler.EXCEPTION_UPDATE_VOTE)));
        } else {
            perform(MockMvcRequestBuilders.put(REST_URL + VOTE_1_ID)
                    .param("restId", Integer.toString(RESTAURANT_3_ID))
                    .with(userHttpBasic(admin)))
                    .andExpect(status().isOk());

            VOTE_MATCHER.assertMatch(voteRepository.getById(VOTE_1_ID, ADMIN_ID).get(), updated);
        }
    }

    @Test
    void createWithLocation() throws Exception {
        Vote newVote = getNew();
        ResultActions action = perform(MockMvcRequestBuilders.post(REST_URL)
                .param("restId", Integer.toString(RESTAURANT_3_ID))
                .with(userHttpBasic(user)))
                .andDo(print())
                .andExpect(status().isCreated());

        Vote created = readFromJson(action, Vote.class);
        int newId = created.id();
        newVote.setId(newId);
        VOTE_MATCHER.assertMatch(created, newVote);
        VOTE_MATCHER.assertMatch(voteRepository.getById(newId, USER_ID).get(), newVote);
    }

    @Test
    void createInvalid() throws Exception {
        perform(MockMvcRequestBuilders.post(REST_URL)
                .param("restId", Integer.toString(NOT_FOUND))
                .with(userHttpBasic(user)))
                .andDo(print())
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void updateInvalid() throws Exception {
        perform(MockMvcRequestBuilders.put(REST_URL + VOTE_1_ID)
                .param("restId", Integer.toString(NOT_FOUND))
                .with(userHttpBasic(user)))
                .andDo(print())
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    void createDuplicate() throws Exception {
        perform(MockMvcRequestBuilders.post(REST_URL)
                .param("restId", Integer.toString(RESTAURANT_1_ID))
                .with(userHttpBasic(admin)))
                .andDo(print())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString(ExceptionInfoHandler.EXCEPTION_DUPLICATE_VOTE)));
    }
}