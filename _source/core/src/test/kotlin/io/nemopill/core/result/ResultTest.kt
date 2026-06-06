package io.nemopill.core.result

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ResultTest {
    @Test
    fun `Ok carries its value`() {
        val result: Result<Int, Result.Err> = Result.Ok(42)
        assertThat(result).isEqualTo(Result.Ok(42))
        assertThat((result as Result.Ok).value).isEqualTo(42)
    }

    @Test
    fun `Err Unexpected carries its non-PII message`() {
        val err = Result.Err.Unexpected("scheduling failed")
        assertThat(err.message).isEqualTo("scheduling failed")
    }

    @Test
    fun `Ok and Err are distinct results`() {
        val ok: Result<Int, Result.Err> = Result.Ok(1)
        val err: Result<Int, Result.Err> = Result.Err.Unexpected("boom")
        assertThat(ok).isNotEqualTo(err)
    }

    @Test
    fun `Ok equality is by value`() {
        assertThat(Result.Ok("x")).isEqualTo(Result.Ok("x"))
        assertThat(Result.Ok("x")).isNotEqualTo(Result.Ok("y"))
    }

    @Test
    fun `Unexpected equality is by message`() {
        assertThat(Result.Err.Unexpected("a")).isEqualTo(Result.Err.Unexpected("a"))
        assertThat(Result.Err.Unexpected("a")).isNotEqualTo(Result.Err.Unexpected("b"))
    }
}
