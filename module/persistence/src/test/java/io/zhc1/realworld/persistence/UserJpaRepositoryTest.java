package io.zhc1.realworld.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import io.zhc1.realworld.model.User;

@DataJpaTest
@DisplayName("UserJpaRepository - JPA User Repository Tests")
class UserJpaRepositoryTest {
    @Autowired
    TestEntityManager entityManager;

    @Autowired
    UserJpaRepository userJpaRepository;

    @Test
    @DisplayName("Should save and find user by ID")
    void whenSaveUser_thenCanFindById() {
        // given
        User user = new User("test@example.com", "testuser", "password123");

        // when
        User savedUser = userJpaRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        // then
        User foundUser = userJpaRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(foundUser.getEmail()).isEqualTo("test@example.com");
        assertThat(foundUser.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should find user by email")
    void whenUserExists_thenFindByEmail() {
        // given
        User user = new User("test@example.com", "testuser", "password123");
        userJpaRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        // when
        User foundUser = userJpaRepository.findByEmail("test@example.com").orElseThrow();

        // then
        assertThat(foundUser.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should find user by username")
    void whenUserExists_thenFindByUsername() {
        // given
        User user = new User("test@example.com", "testuser", "password123");
        userJpaRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        // when
        User foundUser = userJpaRepository.findByUsername("testuser").orElseThrow();

        // then
        assertThat(foundUser.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should return empty when user not found by email")
    void whenUserDoesNotExist_thenFindByEmailReturnsEmpty() {
        // when
        var result = userJpaRepository.findByEmail("nonexistent@example.com");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should check if user exists by email")
    void whenUserExists_thenExistsByEmailReturnsTrue() {
        // given
        User user = new User("test@example.com", "testuser", "password123");
        userJpaRepository.save(user);
        entityManager.flush();

        // when & then
        assertThat(userJpaRepository.existsByEmail("test@example.com")).isTrue();
        assertThat(userJpaRepository.existsByEmail("nonexistent@example.com"))
                .isFalse();
    }

    @Test
    @DisplayName("Should check if user exists by username")
    void whenUserExists_thenExistsByUsernameReturnsTrue() {
        // given
        User user = new User("test@example.com", "testuser", "password123");
        userJpaRepository.save(user);
        entityManager.flush();

        // when & then
        assertThat(userJpaRepository.existsByUsername("testuser")).isTrue();
        assertThat(userJpaRepository.existsByUsername("nonexistent")).isFalse();
    }

    @Test
    @DisplayName("Should check if user exists by email or username")
    void whenUserExists_thenExistsByEmailOrUsernameReturnsTrue() {
        // given
        User user = new User("test@example.com", "testuser", "password123");
        userJpaRepository.save(user);
        entityManager.flush();

        // when & then
        assertThat(userJpaRepository.existsByEmailOrUsername("test@example.com", "anyuser"))
                .isTrue();
        assertThat(userJpaRepository.existsByEmailOrUsername("any@example.com", "testuser"))
                .isTrue();
        assertThat(userJpaRepository.existsByEmailOrUsername(
                        "test@example.com", "testuser"))
                .isTrue();
        assertThat(userJpaRepository.existsByEmailOrUsername("other@example.com", "otheruser"))
                .isFalse();
    }

    @Test
    @DisplayName("Should update user details")
    void whenUpdateUser_thenChangesArePersisted() {
        // given
        User user = new User("test@example.com", "testuser", "password123");
        User savedUser = userJpaRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        // when
        savedUser.setEmail("updated@example.com");
        savedUser.setUsername("updateduser");
        savedUser.setBio("Updated bio");
        savedUser.setImageUrl("https://example.com/image.jpg");
        userJpaRepository.save(savedUser);
        entityManager.flush();
        entityManager.clear();

        // then
        User updatedUser =
                userJpaRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(updatedUser.getUsername()).isEqualTo("updateduser");
        assertThat(updatedUser.getBio()).isEqualTo("Updated bio");
        assertThat(updatedUser.getImageUrl()).isEqualTo("https://example.com/image.jpg");
    }

    @Test
    @DisplayName("Should delete user")
    void whenDeleteUser_thenUserIsRemoved() {
        // given
        User user = new User("test@example.com", "testuser", "password123");
        User savedUser = userJpaRepository.save(user);
        entityManager.flush();

        // when
        userJpaRepository.delete(savedUser);
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(userJpaRepository.findById(savedUser.getId())).isEmpty();
    }
}
