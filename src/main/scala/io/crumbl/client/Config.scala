package io.crumbl.client

import scopt.{DefaultOParserSetup, OParser, OParserBuilder}

/**
 * Config class for command-line parsing
 *
 * @author  Cyril Dever
 * @since   1.0
 * @version 1.0
 */
case class Config(
  c: Boolean = false,
  x: Boolean = false,
  in: Option[String] = None,
  out: Option[String] = None,
  ownerKeys: Option[String] = None,
  ownerSecret: Option[String] = None,
  signerKeys: Option[String] = None,
  signerSecret: Option[String] = None,
  verificationHash: Option[String] = None,
  data: Seq[String] = Seq.empty
) {
  /**
   * @return  the Crumbl&trade; application full version name, eg. 1.0.0
   */
  lazy val appVersion: String = getClass.getPackage.getImplementationVersion

  /**
   * Verify that the passed options and data are appropriate for handling a full Crumbl&trade; process
   */
  def checks: Boolean =  {
    val mode = !(c && x) && (c || x)
    val creation = c &&
      (in.isDefined || (data.length == 1 && !data.head.isEmpty)) &&
      ownerKeys.isDefined &&
      signerKeys.isDefined
    val extraction = x &&
      (
        (ownerKeys.isDefined && ownerSecret.isDefined &&
          verificationHash.isDefined && !verificationHash.getOrElse("").isEmpty &&
          data.nonEmpty
        ) ||
        (signerKeys.isDefined && signerSecret.isDefined)
      )
    val datas = in.isDefined || (in.isEmpty && data.nonEmpty)
    mode && (creation || extraction) && datas
  }

  def isEmpty: Boolean = this == Config.EMPTY
}
object Config {
  val EMPTY: Config = Config()

  private[Config] var _instance: Config = EMPTY

  /**
   * Initialize configuration using command-line arguments
   */
  def init(args: Array[String]): Config = {
    if (_instance.isEmpty) {
      OParser.parse(parser1, args, Config(), new DefaultOParserSetup {
        override def showUsageOnError: Some[Boolean] = Some(true)
      }) match {
        case Some(config) => _instance = config
        case _ =>
          println(getUsage)
          throw new Exception("bad arguments")
      }
    }
    _instance
  }

  /**
   * @return the Config instance after initialization
   */
  def get(): Config = if (!_instance.isEmpty) _instance else throw new Exception("config must be initialize first")

  /**
   * Set a new Config if it checks
   *
   * @return `true` if the passed Config was set as the new instance, `false` otherwise
   */
  def set(c: Config): Boolean = if (c.checks) {
    _instance = c
    true
  } else false

  /**
   * @return the command-line usage text
   */
  def getUsage: String = {
    OParser.usage[Config](parser1)
  }

  private[Config] lazy val builder: OParserBuilder[Config] = OParser.builder[Config]
  private[Config] lazy val parser1: OParser[Unit, Config] = {
    import builder._
    val v = Config().appVersion
    OParser.sequence(
      head("Crumbl", v),
      programName(s"java -cp crumbl-jar-${v}.jar:bcprov-jdk15to18-1.64.jar io.crumbl.Main"),
      opt[Unit]("create")
        .abbr("c")
        .action((_, c) => c.copy(c = true))
        .text("create a crumbled string from source"),
      opt[Unit]("extract")
        .abbr("x")
        .action((_, c) => c.copy(x = true))
        .text("extract crumbl(s)"),
      opt[String]("input")
        .abbr("in")
        .action((x, c) => c.copy(in = Some(x)))
        .text("file to read an existing crumbl from (WARNING: do not add the crumbl string in the command-line arguments too)"),
      opt[String]("output")
        .abbr("out")
        .action((x, c) => c.copy(out = Some(x)))
        .text("file to save result to"),
      opt[String]("owner-keys")
        .action((x, c) => c.copy(ownerKeys = Some(x)))
        .text("comma-separated list of colon-separated encryption algorithm prefix and filepath to public key of owner(s)"),
      opt[String]("owner-secret")
        .action((x, c) => c.copy(ownerSecret = Some(x)))
        .text("filepath to the private key of the owner"),
      opt[String]("signer-keys")
        .action((x, c) => c.copy(signerKeys = Some(x)))
        .text("comma-separated list of colon-separated encryption algorithm prefix and filepath to public key of trusted signer(s)"),
      opt[String]("signer-secret")
        .action((x, c) => c.copy(signerSecret = Some(x)))
        .text("filepath to the private key of the trusted signer"),
      opt[String]("verification-hash")
        .abbr("vh")
        .action((x, c) => c.copy(verificationHash = Some(x)))
        .text("optional verification hash of the data"),
      help("help")
        .abbr("h")
        .text("prints this usage text"),
      arg[String]("<data> ...")
        .unbounded()
        .optional()
        .action((x, c) => c.copy(data = c.data :+ x))
        .text("data to use")
    )
  }
}