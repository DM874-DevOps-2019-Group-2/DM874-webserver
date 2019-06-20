package modules

import com.google.inject.{AbstractModule, Provides}
import play.api.{Configuration, Environment}
import java.io.File

class SecurityModule(environment: Environment, configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
  }
}
