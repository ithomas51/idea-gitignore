// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package mobi.hsz.idea.gitignore.util

import mobi.hsz.idea.gitignore.lang.kind.GitLanguage.Companion.INSTANCE
import mobi.hsz.idea.gitignore.settings.IgnoreSettings
import mobi.hsz.idea.gitignore.settings.IgnoreSettings.UserTemplate
import org.jetbrains.annotations.NonNls
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.StringReader
import java.util.Scanner

/**
 * [Resources] util class that contains methods that work on plugin resources.
 */
object Resources {

    /** Path to the gitignore templates list.  */
    @NonNls
    private val GITIGNORE_TEMPLATES_PATH = "/templates.list"

    /** List of fetched [Template] elements from resources.  */
    private var resourceTemplates: MutableList<Template>? = null// fetch templates from resources

    // fetch user templates
    /**
     * Returns list of gitignore templates.
     *
     * @return Gitignore templates list
     */
    val gitignoreTemplates: List<Template>
        get() {
            val settings = IgnoreSettings.getInstance()
            val starredTemplates: List<String> = settings.starredTemplates
            if (resourceTemplates == null) {
                resourceTemplates = mutableListOf()

                // fetch templates from resources
                try {
                    val list = getResourceContent(GITIGNORE_TEMPLATES_PATH)
                    if (list != null) {
                        val br = BufferedReader(StringReader(list))
                        var line: String
                        while (br.readLine().also { line = it } != null) {
                            line = "/$line"
                            val file = getResource(line)
                            if (file != null) {
                                val content = getResourceContent(line)
                                val template = Template(file, content)
                                template.isStarred = starredTemplates.contains(template.name)
                                resourceTemplates?.add(template)
                            }
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            val templates = mutableListOf<Template>()

            // fetch user templates
            for (userTemplate in settings.userTemplates) {
                templates.add(Template(userTemplate))
            }
            return templates
        }

    /**
     * Returns gitignore templates directory.
     *
     * @return Resources directory
     */
    fun getResource(path: String) = Resources::class.java.getResource(path)?.run { File(path) }

    /**
     * Reads resource file and returns its content as a String.
     *
     * @param path Resource path
     * @return Content
     */
    fun getResourceContent(path: String) = convertStreamToString(Resources::class.java.getResourceAsStream(path))

    /**
     * Converts InputStream resource to String.
     *
     * @param inputStream Input stream
     * @return Content
     */
    private fun convertStreamToString(inputStream: InputStream?) =
        inputStream?.let { stream -> Scanner(stream).useDelimiter("\\A").takeIf { it.hasNext() }?.next() ?: "" }

    /** [Template] entity that defines template fetched from resources or [IgnoreSettings].  */
    class Template : Comparable<Template> {

        /** [File] pointer. `null` if template is fetched from [IgnoreSettings].  */
        val file: File?

        /** Template name.  */
        val name: String

        /** Template content.  */
        val content: String?

        /** Template's [Container].  */
        val container: Container
            get() = if (isStarred) Container.STARRED else field

        /** Template is starred.  */
        var isStarred = false

        /**
         * Defines if template is fetched from resources ([Container.ROOT] directory or [Container.GLOBAL]
         * subdirectory) or is user defined and fetched from [IgnoreSettings].
         */
        enum class Container {
            USER, STARRED, ROOT, GLOBAL
        }

        /**
         * Builds a new instance of [Template]. [Container] will be set to [Container.ROOT] or [ ][Container.GLOBAL] depending on its location.
         *
         * @param file    template's file
         * @param content template's content
         */
        constructor(file: File, content: String?) {
            this.file = file
            name = file.name.replace(INSTANCE.filename, "")
            this.content = content
            container = if (file.parent.endsWith("Global")) Container.GLOBAL else Container.ROOT
        }

        /**
         * Builds a new instance of [Template].
         * [Container] will be set to [Container.USER].
         *
         * @param userTemplate [IgnoreSettings] user template object
         */
        constructor(userTemplate: UserTemplate) {
            file = null
            name = userTemplate.name
            content = userTemplate.content
            container = Container.USER
        }

        /**
         * Returns string representation of [Template].
         *
         * @return template's name
         */
        override fun toString() = name

        /**
         * Compares given template to the current one.
         *
         * @param other template to compare
         * @return templates comparison
         */
        override fun compareTo(other: Template) = name.compareTo(other.name, ignoreCase = true)
    }
}