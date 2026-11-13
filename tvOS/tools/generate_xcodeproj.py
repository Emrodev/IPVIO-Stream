#!/usr/bin/env python3
"""Generate IPVIOStream.xcodeproj without XcodeGen.

Run from the tvOS folder:  python tools/generate_xcodeproj.py
The generated project pins KSPlayer 5.6.2 and targets tvOS 27.
"""
from __future__ import annotations

import hashlib
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "IPVIOStream"
PROJECT = ROOT / "IPVIOStream.xcodeproj"
TARGET_NAME = "IPVIOStream"
BUNDLE_ID = "com.ipvio.stream.tv"
DEPLOYMENT_TARGET = "27.0"


def gid(name: str) -> str:
    return hashlib.md5(name.encode("utf-8")).hexdigest()[:24].upper()


def quote(value: str) -> str:
    if value == "":
        return '""'
    if any(c in value for c in ' "()<>@,;:\\/='):
        return '"' + value.replace('\\', '\\\\').replace('"', '\\"') + '"'
    return value


SWIFT_DIRS = ["App", "Core", "Services", "Player", "UI"]
swift_files = []
for directory in SWIFT_DIRS:
    for path in sorted((APP / directory).glob("*.swift")):
        swift_files.append(path)

assets = APP / "Resources" / "Assets.xcassets"
info_plist = APP / "Resources" / "Info.plist"

main_group = gid("group:main")
product_group = gid("group:products")
app_group = gid("group:app-root")
resources_group = gid("group:resources")
products_ref = gid("product:app")
target_id = gid("target:app")
project_id = gid("project")
project_config_list = gid("configlist:project")
target_config_list = gid("configlist:target")
project_debug = gid("config:project:debug")
project_release = gid("config:project:release")
target_debug = gid("config:target:debug")
target_release = gid("config:target:release")
sources_phase = gid("phase:sources")
frameworks_phase = gid("phase:frameworks")
resources_phase = gid("phase:resources")
package_ref = gid("package:KSPlayer")
package_ks = gid("packageproduct:KSPlayer")
package_ui = gid("packageproduct:KSPlayerUI")
package_ks_build = gid("build:KSPlayer")
package_ui_build = gid("build:KSPlayerUI")

build_files = []
source_build_files = []
for path in swift_files:
    rel = path.relative_to(APP)
    ref = gid("fileref:" + str(rel))
    build = gid("build:" + str(rel))
    source_build_files.append(build)
    build_files.append((build, ref, path.name))

group_refs = {}
for directory in SWIFT_DIRS:
    group_refs[directory] = gid("group:" + directory)

resources_ref = gid("fileref:Resources/Assets.xcassets")
info_ref = gid("fileref:Resources/Info.plist")
assets_build = gid("build:Assets.xcassets")


def file_ref(uid: str, name: str, file_type: str | None = None, path: str | None = None) -> str:
    file_type = file_type or "text"
    path = path or name
    return (
        f"\t\t{uid} /* {name} */ = {{isa = PBXFileReference; "
        f"lastKnownFileType = {file_type}; path = {quote(path)}; sourceTree = \"<group>\"; }};\n"
    )


out = []
out.append("// !$*UTF8*$!\n{\n\tarchiveVersion = 1;\n\tclasses = {\n\t};\n\tobjectVersion = 56;\n\tobjects = {\n")

out.append("\n/* Begin PBXBuildFile section */\n")
for build, ref, name in build_files:
    out.append(f"\t\t{build} /* {name} in Sources */ = {{isa = PBXBuildFile; fileRef = {ref} /* {name} */; }};\n")
out.append(f"\t\t{assets_build} /* Assets.xcassets in Resources */ = {{isa = PBXBuildFile; fileRef = {resources_ref} /* Assets.xcassets */; }};\n")
out.append(f"\t\t{package_ks_build} /* KSPlayer in Frameworks */ = {{isa = PBXBuildFile; productRef = {package_ks} /* KSPlayer */; }};\n")
out.append(f"\t\t{package_ui_build} /* KSPlayerUI in Frameworks */ = {{isa = PBXBuildFile; productRef = {package_ui} /* KSPlayerUI */; }};\n")
out.append("/* End PBXBuildFile section */\n")

out.append("\n/* Begin PBXFileReference section */\n")
out.append(
    f"\t\t{products_ref} /* {TARGET_NAME}.app */ = {{isa = PBXFileReference; explicitFileType = wrapper.application; includeInIndex = 0; path = {TARGET_NAME}.app; sourceTree = BUILT_PRODUCTS_DIR; }};\n"
)
for directory in SWIFT_DIRS:
    for path in sorted((APP / directory).glob("*.swift")):
        rel = path.relative_to(APP)
        out.append(file_ref(gid("fileref:" + str(rel)), path.name, "sourcecode.swift", path.name))
out.append(file_ref(resources_ref, "Assets.xcassets", "folder.assetcatalog", "Assets.xcassets"))
out.append(file_ref(info_ref, "Info.plist", "text.plist.xml", "Info.plist"))
out.append("/* End PBXFileReference section */\n")

out.append("\n/* Begin PBXFrameworksBuildPhase section */\n")
out.append(
    f"\t\t{frameworks_phase} /* Frameworks */ = {{\n"
    "\t\t\tisa = PBXFrameworksBuildPhase;\n"
    "\t\t\tbuildActionMask = 2147483647;\n"
    "\t\t\tfiles = (\n"
    f"\t\t\t\t{package_ks_build} /* KSPlayer in Frameworks */,\n"
    f"\t\t\t\t{package_ui_build} /* KSPlayerUI in Frameworks */,\n"
    "\t\t\t);\n"
    "\t\t\trunOnlyForDeploymentPostprocessing = 0;\n"
    "\t\t};\n"
)
out.append("/* End PBXFrameworksBuildPhase section */\n")

out.append("\n/* Begin PBXGroup section */\n")
out.append(
    f"\t\t{main_group} = {{\n\t\t\tisa = PBXGroup;\n\t\t\tchildren = (\n"
    f"\t\t\t\t{app_group} /* IPVIOStream */,\n"
    f"\t\t\t\t{product_group} /* Products */,\n"
    "\t\t\t);\n\t\t\tsourceTree = \"<group>\";\n\t\t};\n"
)
out.append(
    f"\t\t{app_group} /* IPVIOStream */ = {{\n\t\t\tisa = PBXGroup;\n\t\t\tchildren = (\n"
    + "".join(f"\t\t\t\t{group_refs[d]} /* {d} */,\n" for d in SWIFT_DIRS)
    + f"\t\t\t\t{resources_group} /* Resources */,\n"
    "\t\t\t);\n"
    f"\t\t\tpath = {TARGET_NAME};\n"
    "\t\t\tsourceTree = \"<group>\";\n\t\t};\n"
)
for directory in SWIFT_DIRS:
    out.append(f"\t\t{group_refs[directory]} /* {directory} */ = {{\n\t\t\tisa = PBXGroup;\n\t\t\tchildren = (\n")
    for path in sorted((APP / directory).glob("*.swift")):
        rel = path.relative_to(APP)
        out.append(f"\t\t\t\t{gid('fileref:' + str(rel))} /* {path.name} */,\n")
    out.append("\t\t\t);\n")
    out.append(f"\t\t\tpath = {directory};\n")
    out.append("\t\t\tsourceTree = \"<group>\";\n\t\t};\n")
out.append(
    f"\t\t{resources_group} /* Resources */ = {{\n\t\t\tisa = PBXGroup;\n\t\t\tchildren = (\n"
    f"\t\t\t\t{resources_ref} /* Assets.xcassets */,\n"
    f"\t\t\t\t{info_ref} /* Info.plist */,\n"
    "\t\t\t);\n"
    "\t\t\tpath = Resources;\n"
    "\t\t\tsourceTree = \"<group>\";\n\t\t};\n"
)
out.append(
    f"\t\t{product_group} /* Products */ = {{\n\t\t\tisa = PBXGroup;\n\t\t\tchildren = (\n"
    f"\t\t\t\t{products_ref} /* {TARGET_NAME}.app */,\n"
    "\t\t\t);\n\t\t\tname = Products;\n\t\t\tsourceTree = \"<group>\";\n\t\t};\n"
)
out.append("/* End PBXGroup section */\n")

out.append("\n/* Begin PBXNativeTarget section */\n")
out.append(
    f"\t\t{target_id} /* {TARGET_NAME} */ = {{\n"
    "\t\t\tisa = PBXNativeTarget;\n"
    f"\t\t\tbuildConfigurationList = {target_config_list} /* Build configuration list for PBXNativeTarget \"{TARGET_NAME}\" */;\n"
    "\t\t\tbuildPhases = (\n"
    f"\t\t\t\t{sources_phase} /* Sources */,\n"
    f"\t\t\t\t{frameworks_phase} /* Frameworks */,\n"
    f"\t\t\t\t{resources_phase} /* Resources */,\n"
    "\t\t\t);\n"
    "\t\t\tbuildRules = (\n\t\t\t);\n"
    "\t\t\tdependencies = (\n\t\t\t);\n"
    f"\t\t\tname = {TARGET_NAME};\n"
    f"\t\t\tpackageProductDependencies = (\n"
    f"\t\t\t\t{package_ks} /* KSPlayer */,\n"
    f"\t\t\t\t{package_ui} /* KSPlayerUI */,\n"
    "\t\t\t);\n"
    f"\t\t\tproductName = {TARGET_NAME};\n"
    f"\t\t\tproductReference = {products_ref} /* {TARGET_NAME}.app */;\n"
    "\t\t\tproductType = \"com.apple.product-type.application\";\n"
    "\t\t};\n"
)
out.append("/* End PBXNativeTarget section */\n")

out.append("\n/* Begin PBXProject section */\n")
out.append(
    f"\t\t{project_id} /* Project object */ = {{\n"
    "\t\t\tisa = PBXProject;\n"
    "\t\t\tattributes = {\n"
    "\t\t\t\tBuildIndependentTargetsInParallel = 1;\n"
    "\t\t\t\tLastSwiftUpdateCheck = 2700;\n"
    "\t\t\t\tLastUpgradeCheck = 2700;\n"
    "\t\t\t\tTargetAttributes = {\n"
    f"\t\t\t\t\t{target_id} = {{\n"
    "\t\t\t\t\t\tCreatedOnToolsVersion = 27.0;\n"
    "\t\t\t\t\t};\n"
    "\t\t\t\t};\n"
    "\t\t\t};\n"
    f"\t\t\tbuildConfigurationList = {project_config_list} /* Build configuration list for PBXProject \"{TARGET_NAME}\" */;\n"
    "\t\t\tcompatibilityVersion = \"Xcode 14.0\";\n"
    "\t\t\tdevelopmentRegion = en;\n"
    "\t\t\thasScannedForEncodings = 0;\n"
    "\t\t\tknownRegions = (\n\t\t\t\ten,\n\t\t\t\tBase,\n\t\t\t);\n"
    f"\t\t\tmainGroup = {main_group};\n"
    f"\t\t\tpackageReferences = (\n\t\t\t\t{package_ref} /* XCRemoteSwiftPackageReference \"KSPlayer\" */,\n\t\t\t);\n"
    f"\t\t\tproductRefGroup = {product_group} /* Products */;\n"
    "\t\t\tprojectDirPath = \"\";\n"
    "\t\t\tprojectRoot = \"\";\n"
    "\t\t\ttargets = (\n"
    f"\t\t\t\t{target_id} /* {TARGET_NAME} */,\n"
    "\t\t\t);\n"
    "\t\t};\n"
)
out.append("/* End PBXProject section */\n")

out.append("\n/* Begin PBXResourcesBuildPhase section */\n")
out.append(
    f"\t\t{resources_phase} /* Resources */ = {{\n"
    "\t\t\tisa = PBXResourcesBuildPhase;\n"
    "\t\t\tbuildActionMask = 2147483647;\n"
    "\t\t\tfiles = (\n"
    f"\t\t\t\t{assets_build} /* Assets.xcassets in Resources */,\n"
    "\t\t\t);\n"
    "\t\t\trunOnlyForDeploymentPostprocessing = 0;\n"
    "\t\t};\n"
)
out.append("/* End PBXResourcesBuildPhase section */\n")

out.append("\n/* Begin PBXSourcesBuildPhase section */\n")
out.append(
    f"\t\t{sources_phase} /* Sources */ = {{\n"
    "\t\t\tisa = PBXSourcesBuildPhase;\n"
    "\t\t\tbuildActionMask = 2147483647;\n"
    "\t\t\tfiles = (\n"
)
for build, _, name in build_files:
    out.append(f"\t\t\t\t{build} /* {name} in Sources */,\n")
out.append("\t\t\t);\n\t\t\trunOnlyForDeploymentPostprocessing = 0;\n\t\t};\n")
out.append("/* End PBXSourcesBuildPhase section */\n")

COMMON_PROJECT_SETTINGS = """\t\t\t\tALWAYS_SEARCH_USER_PATHS = NO;
\t\t\t\tCLANG_ANALYZER_NONNULL = YES;
\t\t\t\tCLANG_ANALYZER_NUMBER_OBJECT_CONVERSION = YES_AGGRESSIVE;
\t\t\t\tCLANG_CXX_LANGUAGE_STANDARD = "gnu++20";
\t\t\t\tCLANG_ENABLE_MODULES = YES;
\t\t\t\tCLANG_ENABLE_OBJC_ARC = YES;
\t\t\t\tCLANG_ENABLE_OBJC_WEAK = YES;
\t\t\t\tCLANG_WARN_BLOCK_CAPTURE_AUTORELEASING = YES;
\t\t\t\tCLANG_WARN_BOOL_CONVERSION = YES;
\t\t\t\tCLANG_WARN_COMMA = YES;
\t\t\t\tCLANG_WARN_CONSTANT_CONVERSION = YES;
\t\t\t\tCLANG_WARN_DEPRECATED_OBJC_IMPLEMENTATIONS = YES;
\t\t\t\tCLANG_WARN_DIRECT_OBJC_ISA_USAGE = YES_ERROR;
\t\t\t\tCLANG_WARN_DOCUMENTATION_COMMENTS = YES;
\t\t\t\tCLANG_WARN_EMPTY_BODY = YES;
\t\t\t\tCLANG_WARN_ENUM_CONVERSION = YES;
\t\t\t\tCLANG_WARN_INFINITE_RECURSION = YES;
\t\t\t\tCLANG_WARN_INT_CONVERSION = YES;
\t\t\t\tCLANG_WARN_NON_LITERAL_NULL_CONVERSION = YES;
\t\t\t\tCLANG_WARN_OBJC_IMPLICIT_RETAIN_SELF = YES;
\t\t\t\tCLANG_WARN_OBJC_LITERAL_CONVERSION = YES;
\t\t\t\tCLANG_WARN_OBJC_ROOT_CLASS = YES_ERROR;
\t\t\t\tCLANG_WARN_QUOTED_INCLUDE_IN_FRAMEWORK_HEADER = YES;
\t\t\t\tCLANG_WARN_RANGE_LOOP_ANALYSIS = YES;
\t\t\t\tCLANG_WARN_STRICT_PROTOTYPES = YES;
\t\t\t\tCLANG_WARN_SUSPICIOUS_MOVE = YES;
\t\t\t\tCLANG_WARN_UNGUARDED_AVAILABILITY = YES_AGGRESSIVE;
\t\t\t\tCLANG_WARN_UNREACHABLE_CODE = YES;
\t\t\t\tCLANG_WARN__DUPLICATE_METHOD_MATCH = YES;
\t\t\t\tCOPY_PHASE_STRIP = NO;
\t\t\t\tENABLE_STRICT_OBJC_MSGSEND = YES;
\t\t\t\tGCC_C_LANGUAGE_STANDARD = gnu17;
\t\t\t\tGCC_NO_COMMON_BLOCKS = YES;
\t\t\t\tGCC_WARN_64_TO_32_BIT_CONVERSION = YES;
\t\t\t\tGCC_WARN_ABOUT_RETURN_TYPE = YES_ERROR;
\t\t\t\tGCC_WARN_UNDECLARED_SELECTOR = YES;
\t\t\t\tGCC_WARN_UNINITIALIZED_AUTOS = YES_AGGRESSIVE;
\t\t\t\tGCC_WARN_UNUSED_FUNCTION = YES;
\t\t\t\tGCC_WARN_UNUSED_VARIABLE = YES;
\t\t\t\tLOCALIZATION_PREFERS_STRING_CATALOGS = YES;
\t\t\t\tMTL_FAST_MATH = YES;
\t\t\t\tSDKROOT = appletvos;
"""

out.append("\n/* Begin XCBuildConfiguration section */\n")
out.append(
    f"\t\t{project_debug} /* Debug */ = {{\n"
    "\t\t\tisa = XCBuildConfiguration;\n"
    "\t\t\tbuildSettings = {\n"
    + COMMON_PROJECT_SETTINGS
    + "\t\t\t\tDEBUG_INFORMATION_FORMAT = dwarf;\n"
    "\t\t\t\tENABLE_TESTABILITY = YES;\n"
    "\t\t\t\tGCC_DYNAMIC_NO_PIC = NO;\n"
    "\t\t\t\tGCC_OPTIMIZATION_LEVEL = 0;\n"
    "\t\t\t\tGCC_PREPROCESSOR_DEFINITIONS = (\n\t\t\t\t\t\"DEBUG=1\",\n\t\t\t\t\t\"$(inherited)\",\n\t\t\t\t);\n"
    "\t\t\t\tMTL_ENABLE_DEBUG_INFO = INCLUDE_SOURCE;\n"
    "\t\t\t\tONLY_ACTIVE_ARCH = YES;\n"
    "\t\t\t\tSWIFT_ACTIVE_COMPILATION_CONDITIONS = \"DEBUG $(inherited)\";\n"
    "\t\t\t\tSWIFT_OPTIMIZATION_LEVEL = \"-Onone\";\n"
    "\t\t\t};\n\t\t\tname = Debug;\n\t\t};\n"
)
out.append(
    f"\t\t{project_release} /* Release */ = {{\n"
    "\t\t\tisa = XCBuildConfiguration;\n"
    "\t\t\tbuildSettings = {\n"
    + COMMON_PROJECT_SETTINGS
    + "\t\t\t\tDEBUG_INFORMATION_FORMAT = \"dwarf-with-dsym\";\n"
    "\t\t\t\tENABLE_NS_ASSERTIONS = NO;\n"
    "\t\t\t\tMTL_ENABLE_DEBUG_INFO = NO;\n"
    "\t\t\t\tSWIFT_COMPILATION_MODE = wholemodule;\n"
    "\t\t\t\tSWIFT_OPTIMIZATION_LEVEL = \"-O\";\n"
    "\t\t\t\tVALIDATE_PRODUCT = YES;\n"
    "\t\t\t};\n\t\t\tname = Release;\n\t\t};\n"
)

TARGET_SETTINGS = f"""\t\t\t\tASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME = AccentColor;
\t\t\t\tCODE_SIGN_STYLE = Automatic;
\t\t\t\tCURRENT_PROJECT_VERSION = 1;
\t\t\t\tDEAD_CODE_STRIPPING = YES;
\t\t\t\tDEVELOPMENT_TEAM = "";
\t\t\t\tENABLE_PREVIEWS = YES;
\t\t\t\tENABLE_USER_SCRIPT_SANDBOXING = YES;
\t\t\t\tGENERATE_INFOPLIST_FILE = NO;
\t\t\t\tINFOPLIST_FILE = IPVIOStream/Resources/Info.plist;
\t\t\t\tLD_RUNPATH_SEARCH_PATHS = (
\t\t\t\t\t"$(inherited)",
\t\t\t\t\t"@executable_path/Frameworks",
\t\t\t\t);
\t\t\t\tMARKETING_VERSION = 1.0.0;
\t\t\t\tPRODUCT_BUNDLE_IDENTIFIER = {BUNDLE_ID};
\t\t\t\tPRODUCT_NAME = {TARGET_NAME};
\t\t\t\tSUPPORTED_PLATFORMS = appletvos;
\t\t\t\tSWIFT_EMIT_LOC_STRINGS = NO;
\t\t\t\tSWIFT_STRICT_CONCURRENCY = minimal;
\t\t\t\tSWIFT_VERSION = 5.0;
\t\t\t\tTARGETED_DEVICE_FAMILY = 3;
\t\t\t\tTVOS_DEPLOYMENT_TARGET = {DEPLOYMENT_TARGET};
"""
for config_name, uid in (("Debug", target_debug), ("Release", target_release)):
    out.append(
        f"\t\t{uid} /* {config_name} */ = {{\n"
        "\t\t\tisa = XCBuildConfiguration;\n"
        "\t\t\tbuildSettings = {\n"
        + TARGET_SETTINGS
        + "\t\t\t};\n"
        f"\t\t\tname = {config_name};\n\t\t}};\n"
    )
out.append("/* End XCBuildConfiguration section */\n")

out.append("\n/* Begin XCConfigurationList section */\n")
out.append(
    f"\t\t{project_config_list} /* Build configuration list for PBXProject \"{TARGET_NAME}\" */ = {{\n"
    "\t\t\tisa = XCConfigurationList;\n"
    "\t\t\tbuildConfigurations = (\n"
    f"\t\t\t\t{project_debug} /* Debug */,\n"
    f"\t\t\t\t{project_release} /* Release */,\n"
    "\t\t\t);\n"
    "\t\t\tdefaultConfigurationIsVisible = 0;\n"
    "\t\t\tdefaultConfigurationName = Release;\n"
    "\t\t};\n"
)
out.append(
    f"\t\t{target_config_list} /* Build configuration list for PBXNativeTarget \"{TARGET_NAME}\" */ = {{\n"
    "\t\t\tisa = XCConfigurationList;\n"
    "\t\t\tbuildConfigurations = (\n"
    f"\t\t\t\t{target_debug} /* Debug */,\n"
    f"\t\t\t\t{target_release} /* Release */,\n"
    "\t\t\t);\n"
    "\t\t\tdefaultConfigurationIsVisible = 0;\n"
    "\t\t\tdefaultConfigurationName = Release;\n"
    "\t\t};\n"
)
out.append("/* End XCConfigurationList section */\n")

out.append("\n/* Begin XCRemoteSwiftPackageReference section */\n")
out.append(
    f"\t\t{package_ref} /* XCRemoteSwiftPackageReference \"KSPlayer\" */ = {{\n"
    "\t\t\tisa = XCRemoteSwiftPackageReference;\n"
    "\t\t\trepositoryURL = \"https://github.com/kingslay/KSPlayer.git\";\n"
    "\t\t\trequirement = {\n"
    "\t\t\t\tkind = exactVersion;\n"
    "\t\t\t\tversion = 5.6.2;\n"
    "\t\t\t};\n"
    "\t\t};\n"
)
out.append("/* End XCRemoteSwiftPackageReference section */\n")

out.append("\n/* Begin XCSwiftPackageProductDependency section */\n")
out.append(
    f"\t\t{package_ks} /* KSPlayer */ = {{\n"
    "\t\t\tisa = XCSwiftPackageProductDependency;\n"
    f"\t\t\tpackage = {package_ref} /* XCRemoteSwiftPackageReference \"KSPlayer\" */;\n"
    "\t\t\tproductName = KSPlayer;\n"
    "\t\t};\n"
)
out.append(
    f"\t\t{package_ui} /* KSPlayerUI */ = {{\n"
    "\t\t\tisa = XCSwiftPackageProductDependency;\n"
    f"\t\t\tpackage = {package_ref} /* XCRemoteSwiftPackageReference \"KSPlayer\" */;\n"
    "\t\t\tproductName = KSPlayerUI;\n"
    "\t\t};\n"
)
out.append("/* End XCSwiftPackageProductDependency section */\n")

out.append("\t};\n")
out.append(f"\trootObject = {project_id} /* Project object */;\n}}\n")

PROJECT.mkdir(parents=True, exist_ok=True)
(PROJECT / "project.pbxproj").write_text("".join(out), encoding="utf-8", newline="\n")

# Shared scheme so `xcodebuild -scheme IPVIOStream` works without opening Xcode.
scheme_dir = PROJECT / "xcshareddata" / "xcschemes"
scheme_dir.mkdir(parents=True, exist_ok=True)
scheme = f"""<?xml version="1.0" encoding="UTF-8"?>
<Scheme LastUpgradeVersion = "2700" version = "1.7">
   <BuildAction parallelizeBuildables = "YES" buildImplicitDependencies = "YES">
      <BuildActionEntries>
         <BuildActionEntry buildForTesting = "YES" buildForRunning = "YES" buildForProfiling = "YES" buildForArchiving = "YES" buildForAnalyzing = "YES">
            <BuildableReference
               BuildableIdentifier = "primary"
               BlueprintIdentifier = "{target_id}"
               BuildableName = "{TARGET_NAME}.app"
               BlueprintName = "{TARGET_NAME}"
               ReferencedContainer = "container:{TARGET_NAME}.xcodeproj">
            </BuildableReference>
         </BuildActionEntry>
      </BuildActionEntries>
   </BuildAction>
   <TestAction buildConfiguration = "Debug" selectedDebuggerIdentifier = "Xcode.DebuggerFoundation.Debugger.LLDB" selectedLauncherIdentifier = "Xcode.DebuggerFoundation.Launcher.LLDB" shouldUseLaunchSchemeArgsEnv = "YES">
      <Testables>
      </Testables>
   </TestAction>
   <LaunchAction buildConfiguration = "Debug" selectedDebuggerIdentifier = "Xcode.DebuggerFoundation.Debugger.LLDB" selectedLauncherIdentifier = "Xcode.DebuggerFoundation.Launcher.LLDB" launchStyle = "0" useCustomWorkingDirectory = "NO" ignoresPersistentStateOnLaunch = "NO" debugDocumentVersioning = "YES" debugServiceExtension = "internal" allowLocationSimulation = "YES">
      <BuildableProductRunnable runnableDebuggingMode = "0">
         <BuildableReference
            BuildableIdentifier = "primary"
            BlueprintIdentifier = "{target_id}"
            BuildableName = "{TARGET_NAME}.app"
            BlueprintName = "{TARGET_NAME}"
            ReferencedContainer = "container:{TARGET_NAME}.xcodeproj">
         </BuildableReference>
      </BuildableProductRunnable>
   </LaunchAction>
   <ProfileAction buildConfiguration = "Release" shouldUseLaunchSchemeArgsEnv = "YES" savedToolIdentifier = "" useCustomWorkingDirectory = "NO" debugDocumentVersioning = "YES">
      <BuildableProductRunnable runnableDebuggingMode = "0">
         <BuildableReference
            BuildableIdentifier = "primary"
            BlueprintIdentifier = "{target_id}"
            BuildableName = "{TARGET_NAME}.app"
            BlueprintName = "{TARGET_NAME}"
            ReferencedContainer = "container:{TARGET_NAME}.xcodeproj">
         </BuildableReference>
      </BuildableProductRunnable>
   </ProfileAction>
   <AnalyzeAction buildConfiguration = "Debug">
   </AnalyzeAction>
   <ArchiveAction buildConfiguration = "Release" revealArchiveInOrganizer = "YES">
   </ArchiveAction>
</Scheme>
"""
(scheme_dir / f"{TARGET_NAME}.xcscheme").write_text(scheme, encoding="utf-8", newline="\n")

print(f"Generated {PROJECT / 'project.pbxproj'} with {len(swift_files)} Swift files")
