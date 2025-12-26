@file:OptIn(ExperimentalStoreApi::class)

package io.kusius.letterbox.di

import io.kusius.letterbox.data.MailRemoteLocalDataSource
import io.kusius.letterbox.data.stores.FullMailsStore
import io.kusius.letterbox.data.stores.MailStore
import io.kusius.letterbox.data.stores.MailsStore
import io.kusius.letterbox.domain.MailDataSource
import io.kusius.letterbox.ui.feed.SummaryScreenViewModel
import io.kusius.letterbox.ui.letterbox.LetterboxViewModel
import io.kusius.letterbox.ui.mail.MailScreenViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.mobilenativefoundation.store.core5.ExperimentalStoreApi

val commonModule =
    module {
        single<MailDataSource> {
            MailRemoteLocalDataSource(
                multiMailStore = get(),
                singleMailStore = get(),
                fullMailsStore = get(),
            )
        }

        single {
            MailsStore()
        }

        single { FullMailsStore() }

        single { MailStore() }

        viewModelOf(::MailScreenViewModel)

        viewModelOf(::SummaryScreenViewModel)

        viewModelOf(::LetterboxViewModel)
    }
