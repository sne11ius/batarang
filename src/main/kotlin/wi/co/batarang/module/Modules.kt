package wi.co.batarang.module

import wi.co.batarang.module.activation.ActivationModule
import wi.co.batarang.module.bitbucket.BitbucketModule
import wi.co.batarang.module.jenkins.JenkinsModule
import wi.co.batarang.module.launcher.LauncherModule

val modules = listOf(
    ActivationModule, // This one should always come first, since we tend to missuse the module system
    LauncherModule,
    BitbucketModule,
    JenkinsModule
)
