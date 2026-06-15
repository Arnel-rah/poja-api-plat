package api.poja.io.service.git;

import static api.poja.io.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static api.poja.io.service.event.PojaConfUploadedService.POJA_BOT_USERNAME;
import static java.util.Locale.ROOT;
import static org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM;
import static org.eclipse.jgit.api.ResetCommand.ResetType.HARD;
import static org.eclipse.jgit.transport.RemoteRefUpdate.Status.OK;
import static org.eclipse.jgit.transport.RemoteRefUpdate.Status.UP_TO_DATE;

import api.poja.io.model.exception.ApiException;
import api.poja.io.repository.model.Environment;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;

@Slf4j
@AllArgsConstructor
public class GitUtils {
  private static final String REMOTE_ORIGIN = "origin";
  private static final RefSpec FETCH_ALL_AND_UPDATE_REFSPEC =
      new RefSpec("+refs/heads/*:refs/heads/*");

  public static Git cloneRepository(
      CredentialsProvider ghCredentialsProvider, String repositoryUrl, Path cloneDirPath)
      throws GitAPIException {
    return Git.cloneRepository()
        .setCredentialsProvider(ghCredentialsProvider)
        .setDirectory(cloneDirPath.toFile())
        .setURI(repositoryUrl)
        .setDepth(1)
        .setNoCheckout(true)
        .call();
  }

  public static void pushAndCheckResult(
      CredentialsProvider ghCredentialsProvider, String branchName, Git git)
      throws GitAPIException {
    var results =
        git.push()
            .setRemote(REMOTE_ORIGIN)
            .setRefSpecs(new RefSpec(getFormattedBranchName(branchName)))
            .setCredentialsProvider(ghCredentialsProvider)
            .call();
    for (PushResult r : results) {
      for (RemoteRefUpdate update : r.getRemoteUpdates()) {
        log.info("Having results: {}", update);
        if (update.getStatus() != OK && update.getStatus() != UP_TO_DATE) {
          throw new ApiException(SERVER_EXCEPTION, "Push failed: " + update);
        }
      }
    }
  }

  /** Sets the branch’s upstream on origin. */
  public static void updateLocalUpstream(Git git, String branchName) {
    var config = git.getRepository().getConfig();
    config.setString("branch", branchName, "remote", REMOTE_ORIGIN);
    config.setString("branch", branchName, "merge", "refs/heads/" + branchName);
    try {
      config.save();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String formatShortBranchName(Environment env) {
    return env.getEnvironmentType().name().toLowerCase(ROOT);
  }

  public static void configureGitRepositoryGpg(Git git) {
    StoredConfig storedConfig = git.getRepository().getConfig();
    storedConfig.setString("gpg", null, "format", "openpgp");
  }

  public static boolean doesBranchExist(
      CredentialsProvider ghCredentialsProvider, Git git, String branchName)
      throws GitAPIException {
    var fetchResult = fetchAllRefs(git, ghCredentialsProvider);
    return fetchResult.getAdvertisedRefs().stream()
        .anyMatch(ref -> ref.getName().equals(getFormattedBranchName(branchName)));
  }

  public static String getFormattedBranchName(String branchName) {
    return "refs/heads/" + branchName;
  }

  public static void checkoutBranch(boolean doesBranchExist, Git git, String branchName)
      throws GitAPIException {
    if (!doesBranchExist) {
      log.info("branch does not exist");
      checkoutAndCreateBranch(git, branchName);
    } else {
      git.reset().setRef(branchName).setMode(HARD).call(); // avoid CheckoutConflictException
      git.checkout().setUpstreamMode(SET_UPSTREAM).setName(branchName).call();
    }
  }

  public static void checkoutBranch(
      boolean doesBranchExist, Git git, String branchName, String sourceBranchName)
      throws GitAPIException {
    if (!doesBranchExist) {
      log.info("branch does not exist");
      checkoutAndCreateBranch(git, branchName, sourceBranchName);
    } else {
      git.reset().setRef(branchName).setMode(HARD).call(); // avoid CheckoutConflictException
      git.checkout().setUpstreamMode(SET_UPSTREAM).setName(branchName).call();
    }
  }

  private static void checkoutAndCreateBranch(Git git, String branchName) {
    try {
      git.checkout().setCreateBranch(true).setName(branchName).setUpstreamMode(SET_UPSTREAM).call();
      log.info("successfully created and checked out branch {}", branchName);
    } catch (RefNotFoundException | RefAlreadyExistsException | InvalidRefNameException e) {
      // unreachable because we check for branch existence in remote first then create it with name
      // "PREPROD" or "PROD" which are very valid.
      log.info("RefException ", e);
    } catch (CheckoutConflictException e) {
      // unreachable because this function creates a branch from an existing branch via checkout
      // command, hence no conflict is possible
      log.info("checkoutConflictException", e);
    } catch (GitAPIException e) {
      throw new RuntimeException(e);
    }
  }

  private static void checkoutAndCreateBranch(Git git, String branchName, String sourceBranchName) {
    try {
      if (sourceBranchName != null) {
        git.reset()
            .setRef(sourceBranchName)
            .setMode(HARD)
            .call(); // avoid CheckoutConflictException
      }
      git.checkout()
          .setCreateBranch(true)
          .setStartPoint(sourceBranchName)
          .setName(branchName)
          .setUpstreamMode(SET_UPSTREAM)
          .call();
      log.info(
          "successfully created and checked out branch {} from {} branch",
          branchName,
          sourceBranchName);
    } catch (RefNotFoundException | RefAlreadyExistsException | InvalidRefNameException e) {
      // unreachable because we check for branch existence in remote first then create it with name
      // "PREPROD" or "PROD" which are very valid.
      log.info("RefException ", e);
    } catch (CheckoutConflictException e) {
      // unreachable because this function creates a branch from an existing branch via checkout
      // command, hence no conflict is possible
      log.info("checkoutConflictException", e);
    } catch (GitAPIException e) {
      throw new RuntimeException(e);
    }
  }

  private static FetchResult fetchAllRefs(Git git, CredentialsProvider credentialsProvider)
      throws GitAPIException {
    return git.fetch()
        .setCredentialsProvider(credentialsProvider)
        .setRemote(REMOTE_ORIGIN)
        .setRefSpecs(FETCH_ALL_AND_UPDATE_REFSPEC)
        .call();
  }

  public static void unsignedCommitAsBot(
      Git git, String commitMessage, CredentialsProvider credentialsProvider, boolean isEmpty)
      throws GitAPIException {
    PersonIdent author = new PersonIdent(POJA_BOT_USERNAME, "bot@poja.io");
    git.commit()
        .setMessage(commitMessage)
        .setAuthor(author)
        .setAllowEmpty(isEmpty)
        .setCommitter(author)
        .setCredentialsProvider(credentialsProvider)
        .setSign(false)
        .call();
  }

  public static void gitRm(Git git, List<Path> toRemove) throws GitAPIException {
    var rm = git.rm();
    toRemove.forEach(f -> rm.addFilepattern(String.valueOf(f)));
    rm.call();
  }

  public static void gitAdd(Git git, List<Path> toAdd) throws GitAPIException {
    var add = git.add();
    toAdd.forEach(f -> add.addFilepattern(String.valueOf(f)));
    add.call();
  }
}
