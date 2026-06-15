package api.poja.io.sys.platform;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@AllArgsConstructor
@Slf4j
public class PlatformModeInterceptor {
  private final PlatformConf platformConf;

  @Around("@annotation(saasOnly) || @within(saasOnly)")
  public Object checkSaasMode(ProceedingJoinPoint pjp, SaasOnly saasOnly) throws Throwable {
    if (!platformConf.isSaas()) {
      String className = pjp.getTarget().getClass().getSimpleName();
      String methodName = pjp.getSignature().getName();
      log.info(
          "Skipping {}.{}() — platform mode is {}", className, methodName, platformConf.mode());
      return null;
    }
    return pjp.proceed();
  }
}
